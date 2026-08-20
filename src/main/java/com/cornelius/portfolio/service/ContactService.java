package com.cornelius.portfolio.service;

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

        return new ContactResponse(
            contact.getEmail(), contact.getLinkedinUrl(), contact.getGithubUrl(), contact.getOtherLinks());
    }
}
