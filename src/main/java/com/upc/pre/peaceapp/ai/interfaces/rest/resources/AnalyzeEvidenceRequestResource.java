package com.upc.pre.peaceapp.ai.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for analyzing incident evidence")
public record AnalyzeEvidenceRequestResource(

        @Schema(description = "Evidence URL", example = "https://example.com/evidence/photo.jpg")
        String evidenceUrl,

        @Schema(description = "Evidence type", example = "IMAGE")
        String evidenceType,

        @Schema(description = "Evidence description", example = "Foto tomada despues del incidente")
        String description
) {}
