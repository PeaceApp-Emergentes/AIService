package com.upc.pre.peaceapp.ai.domain.model.valueobjects;

import java.util.List;

public record IncidentClassificationResult(
        String incidentType,
        String severity,
        String summary,
        List<String> recommendedActions,
        boolean mock
) {}
