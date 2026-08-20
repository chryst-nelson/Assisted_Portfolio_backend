package com.cornelius.portfolio.ai;

/**
 * The full set of response shapes the frontend knows how to render as
 * distinct UI (dynamic cards, not just chat bubbles — see PROJECT_PLAN.md
 * section 9). Only TEXT is reachable until tool-calling (M6) gives the
 * assistant real data to attach to the others.
 */
public enum ChatResponseType {
    TEXT,
    PROJECT,
    PROJECT_LIST,
    SKILL_LIST,
    EXPERIENCE,
    CONTACT,
    RESUME,
    LINK
}
