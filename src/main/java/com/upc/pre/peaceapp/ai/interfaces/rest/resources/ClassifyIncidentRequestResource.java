package com.upc.pre.peaceapp.ai.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for classifying an incident")
public record ClassifyIncidentRequestResource(

        @Schema(description = "Incident description", example = "Vi un asalto en la avenida principal")
        String description,

        @Schema(description = "Incident location", example = "Av. Primavera 123")
        String location,

        @Schema(description = "District where the incident happened", example = "San Borja")
        String district
) {}
