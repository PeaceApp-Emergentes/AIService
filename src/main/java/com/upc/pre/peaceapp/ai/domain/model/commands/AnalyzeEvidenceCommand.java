package com.upc.pre.peaceapp.ai.domain.model.commands;

public record AnalyzeEvidenceCommand(
        String evidenceUrl,
        String evidenceType,
        String description
) {
    public AnalyzeEvidenceCommand {
        if ((evidenceUrl == null || evidenceUrl.isBlank()) && (description == null || description.isBlank()))
            throw new IllegalArgumentException("evidenceUrl or description is required");
    }
}
