package com.upc.pre.peaceapp.ai.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Incident classification response")
public record IncidentClassificationResponseResource(
        String incidentType,
        String severity,
        String summary,
        List<String> recommendedActions,
        boolean mock
) {}
