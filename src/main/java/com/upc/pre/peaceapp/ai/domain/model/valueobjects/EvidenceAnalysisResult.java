package com.upc.pre.peaceapp.ai.domain.model.valueobjects;

import java.util.List;

public record EvidenceAnalysisResult(
        String summary,
        List<String> observedSignals,
        boolean requiresHumanReview,
        boolean mock
) {}
