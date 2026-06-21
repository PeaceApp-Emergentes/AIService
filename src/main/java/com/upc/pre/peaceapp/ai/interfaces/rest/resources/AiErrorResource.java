package com.upc.pre.peaceapp.ai.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI service error response")
public record AiErrorResource(
        String code,
        String message,
        boolean mock
) {}
