package com.cornelius.portfolio.ai;

/**
 * The structured envelope actually returned to the frontend. "payload"
 * carries the raw, tool-sourced data for card-worthy response types
 * (SKILL_LIST, PROJECT_LIST, PROJECT, EXPERIENCE, CONTACT, RESUME) —
 * attached by AnswerEngine straight from the
 * portfolio data, so a card always shows exactly what is stored. TEXT responses (including profile, education,
 * certification, and achievement answers, which stay prose-only per the
 * plan's response contract) leave payload null.
 */
public record ChatMessageResponse(ChatResponseType type, String message, Object payload) {}
