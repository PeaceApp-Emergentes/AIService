package com.upc.pre.peaceapp.ai.domain.model.commands;

public record ClassifyIncidentCommand(
        String description,
        String location,
        String district
) {
    public ClassifyIncidentCommand {
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("description cannot be null or empty");
    }
}
