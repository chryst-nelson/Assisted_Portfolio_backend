package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.ai.ChatMessageResponse;
import com.cornelius.portfolio.dto.request.ChatRequest;
import com.cornelius.portfolio.service.ChatOrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private static final long EMITTER_TIMEOUT_MS = 60_000L;
    private static final long CHUNK_DELAY_MS = 12L;

    private final ChatOrchestrationService chatOrchestrationService;

    @PostMapping
    public ChatMessageResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatOrchestrationService.handle(request);
    }


    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        try {
            ChatMessageResponse response = chatOrchestrationService.handle(request);

            for (String chunk : splitIntoChunks(response.message())) {
                emitter.send(SseEmitter.event().name("token").data(chunk));
                sleep(CHUNK_DELAY_MS);
            }

            emitter.send(SseEmitter.event().name("response").data(response, MediaType.APPLICATION_JSON));
            emitter.complete();
        } catch (Exception ex) {
            log.error("Chat stream failed", ex);
            sendErrorEvent(emitter, "Something went wrong. Please try again.");
            emitter.complete();
        }

        return emitter;
    }

    private void sendErrorEvent(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message));
        } catch (Exception ignored) {
            // Connection already broken — nothing more to send.
        }
    }

    private String[] splitIntoChunks(String message) {
        return message.split("(?<= )"); // splits after each space, keeping it with the preceding word
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
