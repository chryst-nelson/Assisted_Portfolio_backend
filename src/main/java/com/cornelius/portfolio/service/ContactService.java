package com.cornelius.portfolio.service;

import com.cornelius.portfolio.dto.request.ContactRequest;
import com.cornelius.portfolio.dto.response.ContactResponse;
import com.cornelius.portfolio.entity.ContactInfo;
import com.cornelius.portfolio.exception.NotFoundException;
import com.cornelius.portfolio.repository.ContactInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactInfoRepository contactInfoRepository;

    @Transactional(readOnly = true)
    public ContactResponse getContact() {
        ContactInfo contact = contactInfoRepository.findTopByOrderByIdAsc()
            .orElseThrow(() -> new NotFoundException("Contact information has not been configured yet."));

        return toResponse(contact);
    }

    // Upsert, same pattern as ProfileService: creates the single contact_info
    // row on first save, updates it thereafter.
    @Transactional
    public ContactResponse updateContact(ContactRequest request) {
        ContactInfo contact = contactInfoRepository.findTopByOrderByIdAsc().orElseGet(ContactInfo::new);

        contact.setEmail(request.email());
        contact.setLinkedinUrl(request.linkedinUrl());
        contact.setGithubUrl(request.githubUrl());
        contact.setOtherLinks(request.otherLinks());

        return toResponse(contactInfoRepository.save(contact));
    }

    private ContactResponse toResponse(ContactInfo contact) {
        return new ContactResponse(
            contact.getEmail(), contact.getLinkedinUrl(), contact.getGithubUrl(), contact.getOtherLinks());
    }
}
