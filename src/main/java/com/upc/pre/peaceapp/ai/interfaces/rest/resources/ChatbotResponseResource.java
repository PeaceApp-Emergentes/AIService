package com.upc.pre.peaceapp.ai.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Chatbot response")
public record ChatbotResponseResource(
        String answer,
        List<String> suggestedActions,
        boolean mock
) {}
