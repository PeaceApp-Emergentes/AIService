package com.upc.pre.peaceapp.ai.interfaces.rest.transform;

import com.upc.pre.peaceapp.ai.domain.model.commands.ChatbotCommand;
import com.upc.pre.peaceapp.ai.interfaces.rest.resources.ChatbotRequestResource;
import org.springframework.stereotype.Component;

@Component
public class ChatbotCommandFromResourceAssembler {

    public ChatbotCommand toCommand(ChatbotRequestResource resource) {
        return new ChatbotCommand(
                resource.message(),
                resource.context(),
                resource.userId()
        );
    }
}
