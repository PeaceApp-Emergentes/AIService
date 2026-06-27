package com.upc.pre.peaceapp.ai.infrastructure.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.pre.peaceapp.ai.domain.model.valueobjects.ChatbotResult;
import com.upc.pre.peaceapp.ai.domain.model.valueobjects.EvidenceAnalysisResult;
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
                Eres el asistente virtual de PeaceApp, una app de seguridad ciudadana.
                Tu unico proposito es ayudar a usar PeaceApp y orientar en seguridad ciudadana.

                RESUMEN DEL PROYECTO:
                PeaceApp tiene dos canales: app movil para ciudadanos y app web para municipalidades.
                El ciudadano (movil) puede registrarse, iniciar sesion, crear reportes de incidentes
                (robo, accidente, zona oscura, acoso, otros), adjuntar evidencia, ver el mapa de reportes y
                zonas de riesgo, recibir alertas de proximidad, compartir ubicacion con contactos y enviar
                alertas de emergencia (SOS). La municipalidad (web) monitorea y gestiona reportes y emergencias.

                REGLAS ESTRICTAS DE ALCANCE:
                DENTRO DEL ALCANCE: uso de PeaceApp y sus funciones, seguridad ciudadana, prevencion,
                reportes de incidentes, zonas de riesgo, evidencia, alertas y emergencias.
                FUERA DEL ALCANCE: matematicas u operaciones (ej. "2+2"), tareas academicas, programacion,
                traducciones, entretenimiento, cultura general, recetas, consejos generales o cualquier
                tema ajeno a PeaceApp.
                Si la consulta esta fuera del alcance, NO la respondas, NO des respuestas parciales,
                NO resuelvas ejercicios y NO escribas resultados de operaciones.
                ADAPTA SIEMPRE la respuesta a la AUDIENCIA segun el Contexto:
                - Si el contexto indica "municipalidad" (app web): el usuario es personal municipal que
                  gestiona y monitorea. Habla en ese tono y usa suggestedActions de gestion, por ejemplo:
                  "Filtra y prioriza reportes por urgencia, tipo o fecha",
                  "Revisa las emergencias en tiempo real del tablero",
                  "Marca un reporte como atendido o aprobado",
                  "Consulta o gestiona tu suscripcion".
                - Si el contexto indica "ciudadano" o no se especifica (app movil): el usuario es un ciudadano.
                  Usa suggestedActions ciudadanas, por ejemplo:
                  "Describe un incidente o situación de riesgo",
                  "Indica ubicación, fecha y hora aproximada",
                  "Usa la opción de reportes si necesitas registrar un hecho".
                Para toda consulta fuera del alcance, el campo answer debe ser EXACTAMENTE:
                "Solo puedo ayudarte con temas relacionados con seguridad ciudadana, prevención y reportes dentro de PeaceApp."
                y suggestedActions debe contener acciones de PeaceApp adecuadas a la audiencia (segun lo anterior).
                Da orientacion prudente. No identifiques personas, no afirmes culpabilidad,
                no reemplaces a autoridades y recomienda contactar a autoridades o municipalidad cuando corresponda.
                Responde solo JSON valido con esta forma:
                {"answer":"texto","suggestedActions":["accion 1","accion 2"]}
                """,
                "Contexto: " + safe(context) + "\nMensaje del usuario: " + safe(message),
                900
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
                Clasifica un reporte ciudadano de seguridad con prudencia. No identifiques personas,
                no afirmes culpabilidad y no reemplaces a autoridades.
                Primero evalua si la descripcion corresponde a un incidente real y comprensible.
                Si el texto es ininteligible, aleatorio, sin sentido, vacio o no describe un incidente
                (por ejemplo "sdfjklsdfsd"), responde "valid": false, usa incidentType "GENERAL_RISK",
                severity "LOW" y en summary explica que la descripcion no permite identificar un incidente valido.
                Si es un incidente valido, responde "valid": true y usa solo uno de estos incidentType:
                ROBBERY, ACCIDENT, DARK_AREA, HARASSMENT, GENERAL_RISK. Usa severity LOW, MEDIUM o HIGH.
                Ademas mejora la redaccion SIN inventar datos nuevos: devuelve suggestedTitle (titulo breve
                y claro, max 60 caracteres) y suggestedDescription. La suggestedDescription debe estar SIEMPRE
                en PRIMERA PERSONA, como si la contara el propio ciudadano (por ejemplo: "Robaron en mi casa
                esta noche..."), con la misma informacion que dio el ciudadano, redactada de forma clara y
                respetuosa, sin agregar hechos que el no haya mencionado.
                Si "valid" es false, deja suggestedTitle y suggestedDescription como "".
                Responde solo JSON valido con esta forma:
                {"valid":true,"incidentType":"GENERAL_RISK","severity":"LOW","summary":"texto","suggestedTitle":"texto","suggestedDescription":"texto","recommendedActions":["accion 1","accion 2"]}
                """,
                "Descripcion: " + safe(description) +
                        "\nUbicacion: " + safe(location) +
                        "\nDistrito: " + safe(district),
                2000
        );

        IncidentAiResponse response = parseJsonPayload(output, IncidentAiResponse.class);
        return new IncidentClassificationResult(
                normalizeIncidentType(response.incidentType()),
                normalizeSeverity(response.severity()),
                safe(response.summary()),
                safe(response.suggestedTitle()),
                safe(response.suggestedDescription()),
                safeList(response.recommendedActions()),
                response.valid() == null || response.valid(),
                false
        );
    }

    public EvidenceAnalysisResult analyzeEvidence(String evidenceUrl, String evidenceType, String description) {
        String output = createVisionResponse(
                """
                Eres un analista de evidencia de PeaceApp (seguridad ciudadana).
                Observa la imagen y decide si es una evidencia valida de un incidente de seguridad ciudadana
                (robo, accidente, zona oscura o falta de iluminacion, acoso u otro riesgo).
                Si la imagen NO corresponde a un incidente (selfie, meme, captura aleatoria, vacia o sin relacion),
                responde "validImage": false y "detectedType": "NONE".
                Si es valida, responde "validImage": true y "detectedType" con uno de:
                ROBBERY, ACCIDENT, DARK_AREA, HARASSMENT, GENERAL_RISK.
                No identifiques personas, no afirmes culpabilidad y no reemplaces a autoridades.
                Responde solo JSON valido con esta forma:
                {"validImage":true,"detectedType":"GENERAL_RISK","summary":"texto","observedSignals":["s1","s2"],"requiresHumanReview":true}
                """,
                "Tipo declarado: " + safe(evidenceType) + "\nDescripcion: " + safe(description),
                evidenceUrl,
                1200
        );

        EvidenceAiResponse response = parseJsonPayload(output, EvidenceAiResponse.class);
        return new EvidenceAnalysisResult(
                normalizeIncidentTypeOrNone(response.detectedType()),
                response.validImage() != null && response.validImage(),
                safe(response.summary()),
                safeList(response.observedSignals()),
                response.requiresHumanReview() == null || response.requiresHumanReview(),
                false
        );
    }

    private String createVisionResponse(String systemPrompt, String userText, String imageUrl, int maxOutputTokens) {
        ensureApiKeyConfigured();

        Map<String, Object> request = Map.of(
                "model", aiProperties.getOpenAiModel(),
                "input", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "input_text", "text", userText),
                                Map.of("type", "input_image", "image_url", imageUrl)
                        ))
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

    private String normalizeIncidentTypeOrNone(String incidentType) {
        if (incidentType == null) return "NONE";

        return switch (incidentType.trim().toUpperCase()) {
            case "ROBBERY", "ACCIDENT", "DARK_AREA", "HARASSMENT", "GENERAL_RISK", "NONE" -> incidentType.trim().toUpperCase();
            default -> "NONE";
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
            Boolean valid,
            String incidentType,
            String severity,
            String summary,
            String suggestedTitle,
            String suggestedDescription,
            List<String> recommendedActions
    ) {}

    private record EvidenceAiResponse(
            Boolean validImage,
            String detectedType,
            String summary,
            List<String> observedSignals,
            Boolean requiresHumanReview
    ) {}
}
