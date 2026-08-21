package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.response.ReindexResponse;
import com.cornelius.portfolio.service.RagIndexingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reindex")
@RequiredArgsConstructor
public class AdminReindexController {

    private final RagIndexingService ragIndexingService;

    @PostMapping
    public ReindexResponse reindex() {
        return new ReindexResponse(ragIndexingService.reindex());
    }
}
