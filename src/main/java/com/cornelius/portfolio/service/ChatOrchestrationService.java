package com.cornelius.portfolio.service;

import com.cornelius.portfolio.ai.ChatMessageResponse;
import com.cornelius.portfolio.ai.ChatModelDecision;
import com.cornelius.portfolio.ai.ToolCallRecorder;
import com.cornelius.portfolio.dto.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Tools are wired as defaults on the shared ChatClient bean (see AiConfig),
 * so every request already has tool access — this class just runs the
 * prompt and reconciles the model's text decision with whatever
 * ToolCallRecorder captured during the tool-calling loop.
 *
 * If a card-worthy tool fired, its type and raw payload win over whatever
 * the model itself claimed — deriving the response type from which tool
 * actually ran is more reliable than trusting the model to self-report it
 * correctly. Conversation history and RAG-backed retrieval are M7/M8.
 */
@Service
@RequiredArgsConstructor
public class ChatOrchestrationService {

    private final ChatClient chatClient;
    private final ToolCallRecorder toolCallRecorder;

    public ChatMessageResponse handle(ChatRequest request) {
        ChatModelDecision decision = chatClient.prompt()
            .user(request.message())
            .call()
            .entity(ChatModelDecision.class);

        if (toolCallRecorder.hasResult()) {
            return new ChatMessageResponse(
                toolCallRecorder.getType(),
                decision.message(),
                toolCallRecorder.getPayload()
            );
        }

        return new ChatMessageResponse(decision.type(), decision.message(), null);
    }
}
