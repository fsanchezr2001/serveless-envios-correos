package com.serveless.envio.service;

import com.serveless.envio.domain.SentEmail;
import com.serveless.envio.dto.SendEmailRequest;
import com.serveless.envio.dto.SendEmailResponse;
import com.serveless.envio.exception.EmailSendException;
import com.serveless.envio.repository.SentEmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Write-then-send decorator around {@link SendGridEmailService}.
 *
 * <p>Every email is persisted before the provider call so no message is lost if
 * the process dies mid-send. Persistence is fail-closed: if the initial insert
 * fails, the email is NOT sent. The row starts as {@code PENDING} and is moved
 * to its terminal state ({@code SENT}/{@code FAILED}) by an independent atomic
 * UPDATE, so the outcome is recorded even though this method itself is not
 * transactional.</p>
 */
@Service
@Primary
public class PersistingEmailService implements EmailService {

	private static final Logger log = LoggerFactory.getLogger(PersistingEmailService.class);
	private static final int ERROR_MESSAGE_MAX_LENGTH = 2000;

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
		} catch (Exception ex) {
			throw new EmailSendException("Failed to persist email intent", 500, ex);
		}

		String providerMessageId;
		try {
			delegate.send(request);
			providerMessageId = delegate.getLastProviderMessageId();
		} catch (RuntimeException ex) {
			markFailed(pending.getId(), ex);
			throw ex;
		}

		repository.markSent(pending.getId(), providerMessageId, Instant.now());
		return new SendEmailResponse(true, "Email sent successfully");
	}

	private void markFailed(String id, Exception ex) {
		try {
			String message = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
			repository.markFailed(id, truncate(message), Instant.now());
		} catch (Exception persistError) {
			// The original send failure is the one worth propagating; the bookkeeping
			// failure must not mask it.
			log.warn("Failed to record FAILED status for email {}", id, persistError);
		}
	}

	private static String truncate(String value) {
		return value.length() <= ERROR_MESSAGE_MAX_LENGTH
				? value
				: value.substring(0, ERROR_MESSAGE_MAX_LENGTH);
	}
}
