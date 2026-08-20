package com.cornelius.portfolio.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class AiConfig {

    @Value("classpath:prompts/system-prompt.txt")
    private Resource systemPromptResource;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) throws IOException {
        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
        return builder.defaultSystem(systemPrompt).build();
    }
}
