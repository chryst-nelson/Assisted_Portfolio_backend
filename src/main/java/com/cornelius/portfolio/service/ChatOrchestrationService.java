package com.cornelius.portfolio.service;

import com.cornelius.portfolio.ai.ChatMessageResponse;
import com.cornelius.portfolio.dto.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * One call in, one structured response out. No intent classification, no
 * tool dispatch, no conversation history — those are M6/M7/M8 concerns.
 * This milestone proves the plumbing: prompt in, structured JSON out.
 */
@Service
@RequiredArgsConstructor
public class ChatOrchestrationService {

    private final ChatClient chatClient;

    public ChatMessageResponse handle(ChatRequest request) {
        return chatClient.prompt()
            .user(request.message())
            .call()
            .entity(ChatMessageResponse.class);
    }
}
