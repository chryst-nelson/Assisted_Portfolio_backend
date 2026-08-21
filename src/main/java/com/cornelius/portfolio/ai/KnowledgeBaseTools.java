package com.cornelius.portfolio.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class KnowledgeBaseTools {

    private static final int TOP_K = 4;
    private static final double SIMILARITY_THRESHOLD = 0.5;

    private final VectorStore vectorStore;

    @Tool(description = "Semantic search over Cornelius's full portfolio knowledge base (profile, project descriptions, and work experience) for open-ended or synthesis questions that don't map to one specific fact — e.g. 'what kind of developer is he', 'has he worked with real-time data', 'what's his overall background'. Prefer the more specific tools (getSkills, getProjects, etc.) when the question is about one clear, specific thing.")
    public String searchKnowledgeBase(
        @ToolParam(description = "The concept or topic to search for")
        String query
    ) {
        List<Document> results = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build()
        );

        if (results.isEmpty()) {
            return "No relevant information found in the knowledge base for this query.";
        }

        return results.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n\n"));
    }
}
