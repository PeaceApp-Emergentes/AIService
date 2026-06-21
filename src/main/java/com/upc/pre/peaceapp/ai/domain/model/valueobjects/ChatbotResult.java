package com.upc.pre.peaceapp.ai.domain.model.valueobjects;

import java.util.List;

public record ChatbotResult(
        String answer,
        List<String> suggestedActions,
        boolean mock
) {}
