package com.cornelius.portfolio.ai;

/**
 * The structured envelope every chat response conforms to. Spring AI's
 * ChatClient#entity(Class) generates JSON-schema format instructions from
 * this record automatically and parses the model's reply back into it — no
 * hand-written schema description needed in the system prompt.
 *
 * Payload attachment (an actual ProjectSummaryResponse list riding along on
 * a PROJECT_LIST response, etc.) is a natural M6 concern: that data can only
 * come from a tool call, and tool-calling doesn't exist yet at this
 * milestone. For now, type is effectively always TEXT.
 */
public record ChatMessageResponse(ChatResponseType type, String message) {}
