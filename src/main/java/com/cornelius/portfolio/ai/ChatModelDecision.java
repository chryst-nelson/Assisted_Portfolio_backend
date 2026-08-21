package com.cornelius.portfolio.ai;

/**
 * What the model generates via ChatClient#entity(Class): a type discriminator
 * and a natural-language reply. Never carries structured data — see
 * ChatMessageResponse for why that's attached separately, not by the model.
 */
public record ChatModelDecision(ChatResponseType type, String message) {}
