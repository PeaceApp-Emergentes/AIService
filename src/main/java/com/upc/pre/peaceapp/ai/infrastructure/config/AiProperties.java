package com.upc.pre.peaceapp.ai.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiProperties {

    @Value("${AI_MOCK_ENABLED:true}")
    private boolean mockEnabled;

    @Value("${OPENAI_API_KEY:}")
    private String openAiApiKey;

    @Value("${OPENAI_MODEL}")
    private String openAiModel;

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public boolean hasOpenAiApiKey() {
        return StringUtils.hasText(openAiApiKey);
    }

    public String getOpenAiApiKey() {
        return openAiApiKey;
    }

    public String getOpenAiModel() {
        return openAiModel;
    }
}
