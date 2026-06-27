package com.upc.pre.peaceapp.ai.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Incident classification response")
public record IncidentClassificationResponseResource(
        String incidentType,
        String severity,
        String summary,
        String suggestedTitle,
        String suggestedDescription,
        List<String> recommendedActions,
        boolean valid,
        boolean mock
) {}
