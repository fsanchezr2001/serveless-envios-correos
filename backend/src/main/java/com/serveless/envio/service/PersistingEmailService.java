package com.serveless.envio.service;

import com.serveless.envio.domain.SentEmail;
import com.serveless.envio.dto.SendEmailRequest;
import com.serveless.envio.dto.SendEmailResponse;
import com.serveless.envio.exception.EmailSendException;
import com.serveless.envio.repository.SentEmailRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Primary
public class PersistingEmailService implements EmailService {

    private final SendGridEmailService delegate;
    private final SentEmailRepository repository;

    public PersistingEmailService(SendGridEmailService delegate, SentEmailRepository repository) {
        this.delegate = delegate;
        this.repository = repository;
    }

    @Override
    public SendEmailResponse send(SendEmailRequest request) {
        SentEmail pending = new SentEmail();
        pending.setTo(request.getTo());
        pending.setToName(request.getToName());
        pending.setSubject(request.getSubject());
        pending.setBody(request.getMessage());
        pending.setStatus("PENDING");
        pending.setSentAt(Instant.now());

        try {
            repository.save(pending);
        } catch (Exception e) {
            throw new EmailSendException("Failed to persist email intent", e);
        }

        String providerMessageId = null;
        try {
            delegate.send(request);
            providerMessageId = delegate.getLastProviderMessageId();
        } catch (Exception ex) {
            try {
                updateFailed(pending.getId(), ex);
            } catch (Exception ignored) {
                // ignore
            }
            if (ex instanceof RuntimeException) {
                throw ex;
            }
            throw new com.serveless.envio.exception.EmailSendException(ex.getMessage(), 500, ex);
        }

        updateSent(pending.getId(), providerMessageId);
        return new SendEmailResponse(true, "Email sent successfully");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateSent(String id, String providerMessageId) {
        repository.findById(id).ifPresent(sent -> {
            sent.setStatus("SENT");
            sent.setProviderMessageId(providerMessageId);
            sent.setSentAt(Instant.now());
            repository.save(sent);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateFailed(String id, Exception ex) {
        repository.findById(id).ifPresent(sent -> {
            sent.setStatus("FAILED");
            sent.setErrorMessage(ex.getMessage() != null ? ex.getMessage() : "Unknown error");
            sent.setSentAt(Instant.now());
            repository.save(sent);
        });
    }
}