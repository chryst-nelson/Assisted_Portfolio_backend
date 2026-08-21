package com.cornelius.portfolio.ai;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Captures the last card-worthy tool result within a single HTTP request.
 * Request-scoped so each /api/chat call gets its own instance with no
 * manual reset between requests — Spring tears it down when the request ends.
 *
 * If a request triggers multiple card-worthy tool calls (e.g. getProjects()
 * to find an ID, then getProjectById() for the detail), only the most recent
 * one wins. That's the semantically correct outcome for a "tell me more
 * about X" follow-up — not a limitation worth solving before it matters.
 */
@Component
@RequestScope
public class ToolCallRecorder {

    private ChatResponseType type;
    private Object payload;

    public void record(ChatResponseType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public boolean hasResult() {
        return type != null;
    }

    public ChatResponseType getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }
}
