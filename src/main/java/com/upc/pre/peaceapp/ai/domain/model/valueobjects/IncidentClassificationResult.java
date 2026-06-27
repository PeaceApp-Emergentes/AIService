package com.upc.pre.peaceapp.ai.domain.model.valueobjects;

import java.util.List;

public record IncidentClassificationResult(
        String incidentType,
        String severity,
        String summary,
        String suggestedTitle,
        String suggestedDescription,
        List<String> recommendedActions,
        boolean valid,
        boolean mock
) {}
