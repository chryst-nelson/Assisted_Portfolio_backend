-- Curated Q&A the chat answers directly, without calling the LLM.
CREATE TABLE faq_entries (
    id BIGSERIAL PRIMARY KEY,
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    keywords VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT true
);
