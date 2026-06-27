package com.upc.pre.peaceapp.ai.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Evidence analysis response")
public record EvidenceAnalysisResponseResource(
        String detectedType,
        boolean validImage,
        String summary,
        List<String> observedSignals,
        boolean requiresHumanReview,
        boolean mock
) {}
