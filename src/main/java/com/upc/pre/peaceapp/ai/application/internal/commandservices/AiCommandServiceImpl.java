package com.upc.pre.peaceapp.ai.application.internal.commandservices;

import com.upc.pre.peaceapp.ai.domain.model.commands.AnalyzeEvidenceCommand;
import com.upc.pre.peaceapp.ai.domain.model.commands.ChatbotCommand;
import com.upc.pre.peaceapp.ai.domain.model.commands.ClassifyIncidentCommand;
import com.upc.pre.peaceapp.ai.domain.model.valueobjects.ChatbotResult;
import com.upc.pre.peaceapp.ai.domain.model.valueobjects.EvidenceAnalysisResult;
import com.upc.pre.peaceapp.ai.domain.model.valueobjects.IncidentClassificationResult;
import com.upc.pre.peaceapp.ai.domain.services.AiCommandService;
import com.upc.pre.peaceapp.ai.infrastructure.config.AiProperties;
import com.upc.pre.peaceapp.ai.infrastructure.openai.OpenAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class AiCommandServiceImpl implements AiCommandService {

    private final AiProperties aiProperties;
    private final OpenAiClient openAiClient;

    public AiCommandServiceImpl(AiProperties aiProperties, OpenAiClient openAiClient) {
        this.aiProperties = aiProperties;
        this.openAiClient = openAiClient;
    }

    @Override
    public ChatbotResult handle(ChatbotCommand command) {
        if (!isClearlyInScopeChatbotMessage(command.message())) {
            log.info("Rejecting out-of-scope chatbot request");
            return outOfScopeChatbotResult();
        }

        if (!aiProperties.isMockEnabled()) {
            log.info("Generating OpenAI chatbot response");
            return openAiClient.generateChatbotResponse(command.message(), command.context());
        }

        log.info("Generating mock chatbot response");

        return new ChatbotResult(
                "Respuesta simulada: mantente en una zona segura, registra detalles relevantes y contacta a las autoridades si hay riesgo inmediato.",
                List.of("Verificar la ubicacion", "Registrar evidencia disponible", "Reportar el incidente desde PeaceApp"),
                true
        );
    }

    private boolean isClearlyInScopeChatbotMessage(String message) {
        String normalized = normalizeForScopeCheck(message);
        if (normalized.isBlank()) return false;

        return containsAny(normalized,
                "seguridad", "ciudadana", "prevencion", "prevenir",
                "reporte", "reportar", "reportes", "incidente", "incidentes", "hecho",
                "riesgo", "riesgos", "peligro", "peligroso", "emergencia", "emergencias",
                "evidencia", "evidencias", "foto", "video", "audio", "prueba", "pruebas",
                "robo", "robaron", "asaltaron", "asalto", "ladron", "hurto",
                "acoso", "acosaron", "accidente", "choque", "atropello",
                "violencia", "amenaza", "amenazaron", "sospechoso", "sospechosa",
                "forzando", "forzar", "puerta", "ventana", "cerradura",
                "oscuridad", "iluminacion", "zona oscura",
                "policia", "serenazgo", "municipalidad", "autoridad", "autoridades",
                "ubicacion", "fecha", "hora", "direccion",
                "peaceapp", "aplicacion", "clasificar", "clasificacion",
                "redactar reporte", "crear reporte", "registrar reporte", "registrar un hecho");
    }

    private String normalizeForScopeCheck(String value) {
        String safeValue = safe(value).toLowerCase(Locale.ROOT);
        return Normalizer.normalize(safeValue, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) return true;
        }

        return false;
    }

    private ChatbotResult outOfScopeChatbotResult() {
        return new ChatbotResult(
                "Solo puedo ayudarte con temas relacionados con seguridad ciudadana, prevenci\u00f3n y reportes dentro de PeaceApp.",
                List.of(
                        "Describe un incidente o situaci\u00f3n de riesgo",
                        "Indica ubicaci\u00f3n, fecha y hora aproximada",
                        "Usa la opci\u00f3n de reportes si necesitas registrar un hecho"
                ),
                false
        );
    }

    @Override
    public IncidentClassificationResult handle(ClassifyIncidentCommand command) {
        if (!aiProperties.isMockEnabled()) {
            log.info("Generating OpenAI incident classification");
            return openAiClient.classifyIncident(command.description(), command.location(), command.district());
        }

        log.info("Generating mock incident classification");

        String content = safe(command.description()) + " " + safe(command.location()) + " " + safe(command.district());
        String normalized = content.toLowerCase();

        if (normalized.contains("robo") || normalized.contains("asalto") || normalized.contains("theft") || normalized.contains("robbery")) {
            return buildIncidentClassification("ROBBERY", "HIGH", "Posible incidente de robo o asalto reportado.");
        }
        if (normalized.contains("acoso") || normalized.contains("harassment")) {
            return buildIncidentClassification("HARASSMENT", "MEDIUM", "Posible incidente de acoso reportado.");
        }
        if (normalized.contains("accidente") || normalized.contains("choque") || normalized.contains("crash")) {
            return buildIncidentClassification("ACCIDENT", "HIGH", "Posible accidente reportado.");
        }

        return buildIncidentClassification("GENERAL_RISK", "LOW", "Incidente general pendiente de revision.");
    }

    @Override
    public EvidenceAnalysisResult handle(AnalyzeEvidenceCommand command) {
        if (!aiProperties.isMockEnabled()) {
            log.info("Returning controlled evidence analysis response without vision");
            return new EvidenceAnalysisResult(
                    "Analisis visual real aun no implementado. La evidencia fue recibida y debe ser revisada por un operador.",
                    List.of("Revision humana requerida", "Vision real pendiente de implementacion"),
                    true,
                    false
            );
        }

        log.info("Generating mock evidence analysis");

        return new EvidenceAnalysisResult(
                "Analisis simulado: la evidencia fue recibida y debe ser revisada por un operador antes de tomar decisiones.",
                List.of("Evidencia disponible", "Descripcion asociada al reporte", "Revision humana recomendada"),
                true,
                true
        );
    }

    private IncidentClassificationResult buildIncidentClassification(String type, String severity, String summary) {
        return new IncidentClassificationResult(
                type,
                severity,
                summary,
                List.of("Confirmar datos del reporte", "Validar ubicacion", "Escalar si existe riesgo inmediato"),
                true
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
