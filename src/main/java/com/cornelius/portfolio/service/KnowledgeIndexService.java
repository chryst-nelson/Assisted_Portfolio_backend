package com.cornelius.portfolio.service;

import com.cornelius.portfolio.ai.KnowledgeBase;
import org.springframework.stereotype.Service;

/**
 * Forces the chat assistant to re-read the portfolio from the database
 * (POST /api/admin/reindex). The assistant also refreshes itself every couple
 * of minutes, so this is only needed to see an edit immediately.
 */
@Service
public class KnowledgeIndexService {

    private final KnowledgeBase knowledgeBase;

    public KnowledgeIndexService(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /** @return number of searchable passages now loaded */
    public int reindex() {
        return knowledgeBase.rebuild().passages().size();
    }
}
