package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.ai.ChatMessageResponse;
import com.cornelius.portfolio.dto.request.ChatRequest;
import com.cornelius.portfolio.service.ChatOrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatOrchestrationService chatOrchestrationService;

    @PostMapping
    public ChatMessageResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatOrchestrationService.handle(request);
    }
}
