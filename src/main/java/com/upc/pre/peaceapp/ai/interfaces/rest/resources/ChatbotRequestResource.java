package com.upc.pre.peaceapp.ai.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for the PeaceApp chatbot")
public record ChatbotRequestResource(

        @Schema(description = "User message", example = "Hay un robo cerca de mi ubicacion, que debo hacer?")
        String message,

        @Schema(description = "Optional contextual information", example = "Citizen is near Miraflores")
        String context,

        @Schema(description = "Optional PeaceApp user ID", example = "101")
        Long userId
) {}
