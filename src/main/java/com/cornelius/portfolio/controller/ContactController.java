package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.response.ContactResponse;
import com.cornelius.portfolio.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public ContactResponse getContact() {
        return contactService.getContact();
    }
}
