package com.cornelius.portfolio.service;

import com.cornelius.portfolio.ai.ChatMessageResponse;
import com.cornelius.portfolio.ai.PromptInjectionGuard;
import com.cornelius.portfolio.dto.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Entry point for a chat message. There is no language model behind it: the
 * answer is assembled by {@link AnswerEngine} from the owner's published data.
 */
@Service
@RequiredArgsConstructor
public class ChatOrchestrationService {

    private final PromptInjectionGuard promptInjectionGuard;
    private final AnswerEngine answerEngine;

    public ChatMessageResponse handle(ChatRequest request) {
        promptInjectionGuard.check(request.message());
        return answerEngine.answer(request.message(), request.mode());
    }
}
