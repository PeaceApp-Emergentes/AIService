package com.upc.pre.peaceapp.ai.interfaces.rest;

import com.upc.pre.peaceapp.ai.domain.services.AiCommandService;
import com.upc.pre.peaceapp.ai.interfaces.rest.resources.AiErrorResource;
import com.upc.pre.peaceapp.ai.interfaces.rest.resources.AnalyzeEvidenceRequestResource;
import com.upc.pre.peaceapp.ai.interfaces.rest.resources.ChatbotRequestResource;
import com.upc.pre.peaceapp.ai.interfaces.rest.resources.ClassifyIncidentRequestResource;
import com.upc.pre.peaceapp.ai.interfaces.rest.resources.EvidenceAnalysisResponseResource;
import com.upc.pre.peaceapp.ai.interfaces.rest.resources.ChatbotResponseResource;
import com.upc.pre.peaceapp.ai.interfaces.rest.resources.IncidentClassificationResponseResource;
import com.upc.pre.peaceapp.ai.interfaces.rest.transform.AnalyzeEvidenceCommandFromResourceAssembler;
import com.upc.pre.peaceapp.ai.interfaces.rest.transform.ChatbotCommandFromResourceAssembler;
import com.upc.pre.peaceapp.ai.interfaces.rest.transform.ClassifyIncidentCommandFromResourceAssembler;
import com.upc.pre.peaceapp.ai.interfaces.rest.transform.EvidenceAnalysisResponseResourceFromResultAssembler;
import com.upc.pre.peaceapp.ai.interfaces.rest.transform.ChatbotResponseResourceFromResultAssembler;
import com.upc.pre.peaceapp.ai.interfaces.rest.transform.IncidentClassificationResponseResourceFromResultAssembler;
import com.upc.pre.peaceapp.ai.infrastructure.openai.OpenAiProviderException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/ai", produces = APPLICATION_JSON_VALUE)
@Tag(name = "AI", description = "Operations related to PeaceApp AI features")
@Slf4j
public class AiController {

    private final AiCommandService aiCommandService;
    private final ChatbotCommandFromResourceAssembler chatbotCommandFromResourceAssembler;
    private final ClassifyIncidentCommandFromResourceAssembler classifyIncidentCommandFromResourceAssembler;
    private final AnalyzeEvidenceCommandFromResourceAssembler analyzeEvidenceCommandFromResourceAssembler;

    public AiController(AiCommandService aiCommandService,
                        ChatbotCommandFromResourceAssembler chatbotCommandFromResourceAssembler,
                        ClassifyIncidentCommandFromResourceAssembler classifyIncidentCommandFromResourceAssembler,
                        AnalyzeEvidenceCommandFromResourceAssembler analyzeEvidenceCommandFromResourceAssembler) {
        this.aiCommandService = aiCommandService;
        this.chatbotCommandFromResourceAssembler = chatbotCommandFromResourceAssembler;
        this.classifyIncidentCommandFromResourceAssembler = classifyIncidentCommandFromResourceAssembler;
        this.analyzeEvidenceCommandFromResourceAssembler = analyzeEvidenceCommandFromResourceAssembler;
    }

    @Operation(summary = "Chatbot assistance", description = "Returns safety assistance for a user message.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chatbot response generated",
                    content = @Content(mediaType = APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ChatbotResponseResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "503", description = "AI provider is not configured")
    })
    @PostMapping(value = "/chatbot", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> chatbot(@RequestBody ChatbotRequestResource resource) {
        log.info("Processing chatbot request");

        try {
            var command = chatbotCommandFromResourceAssembler.toCommand(resource);
            var result = aiCommandService.handle(command);
            return ResponseEntity.ok(ChatbotResponseResourceFromResultAssembler.toResourceFromResult(result));
        } catch (Exception e) {
            return handleAiException(e);
        }
    }

    @Operation(summary = "Classify incident", description = "Classifies an incident report using AI service rules.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incident classified",
                    content = @Content(mediaType = APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = IncidentClassificationResponseResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "503", description = "AI provider is not configured")
    })
    @PostMapping(value = "/classify-incident", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> classifyIncident(@RequestBody ClassifyIncidentRequestResource resource) {
        log.info("Processing incident classification request");

        try {
            var command = classifyIncidentCommandFromResourceAssembler.toCommand(resource);
            var result = aiCommandService.handle(command);
            return ResponseEntity.ok(IncidentClassificationResponseResourceFromResultAssembler.toResourceFromResult(result));
        } catch (Exception e) {
            return handleAiException(e);
        }
    }

    @Operation(summary = "Analyze evidence", description = "Analyzes incident evidence metadata using AI service rules.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evidence analyzed",
                    content = @Content(mediaType = APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EvidenceAnalysisResponseResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "503", description = "AI provider is not configured")
    })
    @PostMapping(value = "/analyze-evidence", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> analyzeEvidence(@RequestBody AnalyzeEvidenceRequestResource resource) {
        log.info("Processing evidence analysis request");

        try {
            var command = analyzeEvidenceCommandFromResourceAssembler.toCommand(resource);
            var result = aiCommandService.handle(command);
            return ResponseEntity.ok(EvidenceAnalysisResponseResourceFromResultAssembler.toResourceFromResult(result));
        } catch (Exception e) {
            return handleAiException(e);
        }
    }

    private ResponseEntity<AiErrorResource> handleAiException(Exception e) {
        if (e instanceof OpenAiProviderException providerException) {
            log.error("OpenAI provider error: {}", providerException.getMessage());
            return ResponseEntity.status(providerException.getStatus())
                    .body(new AiErrorResource(providerException.getCode(), providerException.getMessage(), false));
        }

        if (e instanceof IllegalArgumentException) {
            log.error("Invalid AI request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AiErrorResource("INVALID_AI_REQUEST", e.getMessage(), false));
        }

        if (e instanceof IllegalStateException) {
            log.error("AI provider configuration error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new AiErrorResource("AI_PROVIDER_NOT_CONFIGURED", e.getMessage(), false));
        }

        if (e instanceof UnsupportedOperationException) {
            log.error("AI provider not implemented: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(new AiErrorResource("AI_PROVIDER_NOT_IMPLEMENTED", e.getMessage(), false));
        }

        log.error("Unexpected AI service error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AiErrorResource("AI_SERVICE_ERROR", "Unexpected error processing AI request", false));
    }
}
