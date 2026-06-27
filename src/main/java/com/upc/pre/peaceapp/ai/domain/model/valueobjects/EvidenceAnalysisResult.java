package com.upc.pre.peaceapp.ai.domain.model.valueobjects;

import java.util.List;

public record EvidenceAnalysisResult(
        String detectedType,
        boolean validImage,
        String summary,
        List<String> observedSignals,
        boolean requiresHumanReview,
        boolean mock
) {}
