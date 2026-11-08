package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.request.ContactRequest;
import com.cornelius.portfolio.dto.response.ContactResponse;
import com.cornelius.portfolio.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/contact")
@RequiredArgsConstructor
public class AdminContactController {

    private final ContactService contactService;

    @PutMapping
    public ContactResponse update(@Valid @RequestBody ContactRequest request) {
        return contactService.updateContact(request);
    }
}
