package com.upc.pre.peaceapp.ai.interfaces.rest.transform;

import com.upc.pre.peaceapp.ai.domain.model.valueobjects.ChatbotResult;
import com.upc.pre.peaceapp.ai.interfaces.rest.resources.ChatbotResponseResource;

public class ChatbotResponseResourceFromResultAssembler {

    public static ChatbotResponseResource toResourceFromResult(ChatbotResult result) {
        return new ChatbotResponseResource(
                result.answer(),
                result.suggestedActions(),
                result.mock()
        );
    }
}
