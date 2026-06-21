package com.upc.pre.peaceapp.ai.infrastructure.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.pre.peaceapp.ai.domain.model.valueobjects.ChatbotResult;
import com.upc.pre.peaceapp.ai.domain.model.valueobjects.IncidentClassificationResult;
import com.upc.pre.peaceapp.ai.infrastructure.config.AiProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private static final String RESPONSES_PATH = "/responses";

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiClient(AiProperties aiProperties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    public ChatbotResult generateChatbotResponse(String message, String context) {
        String output = createResponse(
                """
                Eres el asistente de seguridad ciudadana de PeaceApp.
                Antes de responder, decide si la consulta esta dentro del alcance de PeaceApp.
                DENTRO DEL ALCANCE: seguridad ciudadana, prevencion, reportes de incidentes, riesgos,
                evidencia, orientacion ante emergencias, uso de PeaceApp y registro objetivo de informacion.
                FUERA DEL ALCANCE: tareas academicas, matematicas, programacion, entretenimiento,
                cultura general, consejos generales o cualquier tema no relacionado con seguridad ciudadana
                o reportes dentro de PeaceApp.
                Si la consulta esta fuera del alcance, NO respondas su contenido, NO des respuestas parciales,
                NO resuelvas ejercicios y NO digas resultados de operaciones matematicas.
                Para toda consulta fuera del alcance, el campo answer debe ser exactamente:
                "Solo puedo ayudarte con temas relacionados con seguridad ciudadana, prevención y reportes dentro de PeaceApp."
                y suggestedActions debe contener acciones relacionadas con PeaceApp, por ejemplo:
                "Describe un incidente o situación de riesgo",
                "Indica ubicación, fecha y hora aproximada",
                "Usa la opción de reportes si necesitas registrar un hecho".
                Da orientacion preventiva y prudente. No identifiques personas, no afirmes culpabilidad,
                no reemplaces a autoridades y recomienda contactar a autoridades o municipalidad cuando corresponda.
                Responde solo JSON valido con esta forma:
                {"answer":"texto","suggestedActions":["accion 1","accion 2"]}
                """,
                "Contexto: " + safe(context) + "\nMensaje del ciudadano: " + safe(message),
                600
        );

        ChatbotAiResponse response = parseJsonPayload(output, ChatbotAiResponse.class);
        return new ChatbotResult(
                safe(response.answer()),
                safeList(response.suggestedActions()),
                false
        );
    }

    public IncidentClassificationResult classifyIncident(String description, String location, String district) {
        String output = createResponse(
                """
                Clasifica un reporte ciudadano con prudencia. No identifiques personas, no afirmes culpabilidad
                y no reemplaces a autoridades. Usa solo uno de estos incidentType:
                ROBBERY, ACCIDENT, DARK_AREA, HARASSMENT, GENERAL_RISK.
                Usa severity LOW, MEDIUM o HIGH. Responde solo JSON valido con esta forma:
                {"incidentType":"GENERAL_RISK","severity":"LOW","summary":"texto","recommendedActions":["accion 1","accion 2"]}
                """,
                "Descripcion: " + safe(description) +
                        "\nUbicacion: " + safe(location) +
                        "\nDistrito: " + safe(district),
                500
        );

        IncidentAiResponse response = parseJsonPayload(output, IncidentAiResponse.class);
        return new IncidentClassificationResult(
                normalizeIncidentType(response.incidentType()),
                normalizeSeverity(response.severity()),
                safe(response.summary()),
                safeList(response.recommendedActions()),
                false
        );
    }

    private String createResponse(String systemPrompt, String userPrompt, int maxOutputTokens) {
        ensureApiKeyConfigured();

        Map<String, Object> request = Map.of(
                "model", aiProperties.getOpenAiModel(),
                "input", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_output_tokens", maxOutputTokens
        );

        try {
            String responseBody = restClient.post()
                    .uri(RESPONSES_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getOpenAiApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            return extractOutputText(responseBody);
        } catch (RestClientResponseException e) {
            throw mapOpenAiHttpError(e);
        } catch (ResourceAccessException e) {
            throw new OpenAiProviderException(
                    "AI_PROVIDER_NETWORK_ERROR",
                    "Could not connect to OpenAI provider",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }

    private void ensureApiKeyConfigured() {
        if (!aiProperties.hasOpenAiApiKey()) {
            throw new IllegalStateException("OpenAI API key is required when AI_MOCK_ENABLED=false");
        }
    }

    private OpenAiProviderException mapOpenAiHttpError(RestClientResponseException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        String providerMessage = extractProviderMessage(e.getResponseBodyAsString());
        String normalizedBody = e.getResponseBodyAsString() == null
                ? ""
                : e.getResponseBodyAsString().toLowerCase();

        if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
            return new OpenAiProviderException(
                    "AI_PROVIDER_AUTHENTICATION_FAILED",
                    "OpenAI rejected the configured API key",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }

        if (e.getStatusCode().value() == 429 && normalizedBody.contains("insufficient_quota")) {
            return new OpenAiProviderException(
                    "AI_PROVIDER_QUOTA_EXCEEDED",
                    "OpenAI quota or billing is insufficient",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        if (e.getStatusCode().value() == 429) {
            return new OpenAiProviderException(
                    "AI_PROVIDER_RATE_LIMITED",
                    "OpenAI rate limit reached. Try again later",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        if (e.getStatusCode().value() == 404 || normalizedBody.contains("model")) {
            return new OpenAiProviderException(
                    "AI_PROVIDER_MODEL_UNAVAILABLE",
                    "Configured OpenAI model is not available",
                    HttpStatus.BAD_GATEWAY
            );
        }

        return new OpenAiProviderException(
                "AI_PROVIDER_ERROR",
                providerMessage.isBlank() ? "OpenAI provider returned an error" : providerMessage,
                status == null ? HttpStatus.BAD_GATEWAY : status
        );
    }

    private String extractProviderMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return "";

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("error").path("message").asText("");
        } catch (JsonProcessingException ignored) {
            return "";
        }
    }

    private String extractOutputText(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw unexpectedResponse();
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String directOutput = root.path("output_text").asText("");
            if (!directOutput.isBlank()) return directOutput;

            StringBuilder builder = new StringBuilder();
            for (JsonNode outputItem : root.path("output")) {
                for (JsonNode contentItem : outputItem.path("content")) {
                    String text = contentItem.path("text").asText("");
                    if (!text.isBlank()) builder.append(text);
                }
            }

            if (builder.isEmpty()) throw unexpectedResponse();
            return builder.toString();
        } catch (JsonProcessingException e) {
            throw unexpectedResponse();
        }
    }

    private <T> T parseJsonPayload(String rawOutput, Class<T> targetType) {
        String json = extractJsonObject(rawOutput);

        try {
            return objectMapper.readValue(json, targetType);
        } catch (JsonProcessingException e) {
            throw new OpenAiProviderException(
                    "AI_PROVIDER_UNEXPECTED_RESPONSE",
                    "OpenAI returned a response with an unexpected format",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    private String extractJsonObject(String rawOutput) {
        if (rawOutput == null) throw unexpectedResponse();

        String trimmed = rawOutput.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*", "")
                    .replaceFirst("```$", "")
                    .trim();
        }

        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace < 0 || lastBrace < firstBrace) throw unexpectedResponse();

        return trimmed.substring(firstBrace, lastBrace + 1);
    }

    private OpenAiProviderException unexpectedResponse() {
        return new OpenAiProviderException(
                "AI_PROVIDER_UNEXPECTED_RESPONSE",
                "OpenAI returned a response with an unexpected format",
                HttpStatus.BAD_GATEWAY
        );
    }

    private String normalizeIncidentType(String incidentType) {
        if (incidentType == null) return "GENERAL_RISK";

        return switch (incidentType.trim().toUpperCase()) {
            case "ROBBERY", "ACCIDENT", "DARK_AREA", "HARASSMENT", "GENERAL_RISK" -> incidentType.trim().toUpperCase();
            default -> "GENERAL_RISK";
        };
    }

    private String normalizeSeverity(String severity) {
        if (severity == null) return "LOW";

        return switch (severity.trim().toUpperCase()) {
            case "LOW", "MEDIUM", "HIGH" -> severity.trim().toUpperCase();
            default -> "LOW";
        };
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record ChatbotAiResponse(
            String answer,
            List<String> suggestedActions
    ) {}

    private record IncidentAiResponse(
            String incidentType,
            String severity,
            String summary,
            List<String> recommendedActions
    ) {}
}
