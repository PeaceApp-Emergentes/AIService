package com.upc.pre.peaceapp.ai.domain.model.commands;

public record ChatbotCommand(
        String message,
        String context,
        Long userId
) {
    public ChatbotCommand {
        if (message == null || message.isBlank())
            throw new IllegalArgumentException("message cannot be null or empty");
    }
}
