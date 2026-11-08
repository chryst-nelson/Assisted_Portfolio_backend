package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.request.FaqRequest;
import com.cornelius.portfolio.dto.response.FaqResponse;
import com.cornelius.portfolio.service.FaqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/faq")
@RequiredArgsConstructor
public class AdminFaqController {

    private final FaqService faqService;

    @GetMapping
    public List<FaqResponse> getAll() {
        return faqService.getAllForAdmin();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FaqResponse create(@Valid @RequestBody FaqRequest request) {
        return faqService.create(request);
    }

    @PutMapping("/{id}")
    public FaqResponse update(@PathVariable Long id, @Valid @RequestBody FaqRequest request) {
        return faqService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        faqService.delete(id);
    }
}
