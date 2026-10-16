package com.serveless.envio.service;

import com.serveless.envio.model.EmailLog;
import com.serveless.envio.dto.SendEmailRequest;
import com.serveless.envio.dto.SendEmailResponse;
import com.serveless.envio.exception.EmailSendException;
import com.serveless.envio.repository.EmailLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersistingEmailServiceTest {

	private final SendGridEmailService delegate = mock(SendGridEmailService.class);
	private final EmailLogRepository repository = mock(EmailLogRepository.class);
	private final PersistingEmailService service = new PersistingEmailService(delegate, repository);

	private SendEmailRequest buildRequest() {
		SendEmailRequest request = new SendEmailRequest();
		request.setTo("recipient@example.com");
		request.setToName("Recipient");
		request.setSubject("Test subject");
		request.setMessage("<p>Hello from test</p>");
		return request;
	}

	@Test
	void persistsPendingIntentBeforeCallingTheProvider() {
		service.send(buildRequest());

		ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
		verify(repository).save(captor.capture());

		EmailLog persisted = captor.getValue();
		assertEquals("PENDING", persisted.getStatus());
		assertEquals("recipient@example.com", persisted.getTo());
		assertEquals("<p>Hello from test</p>", persisted.getBody());
	}

	@Test
	void recordsProviderMessageIdWhenSendSucceeds() {
		when(delegate.getLastProviderMessageId()).thenReturn("msg-123");

		SendEmailResponse response = service.send(buildRequest());

		assertTrue(response.isSuccess());
		verify(repository).markSent(anyString(), eq("msg-123"), any(Instant.class));
		verify(repository, never()).markFailed(anyString(), anyString(), any(Instant.class));
	}

	@Test
	void doesNotCallProviderWhenPersistFails() {
		when(repository.save(any(EmailLog.class))).thenThrow(new RuntimeException("db down"));

		EmailSendException exception = assertThrows(EmailSendException.class, () -> service.send(buildRequest()));

		assertEquals(500, exception.getStatusCode());
		verify(delegate, never()).send(any(SendEmailRequest.class));
	}

	@Test
	void marksRowFailedAndPropagatesOriginalErrorWhenProviderFails() {
		when(delegate.send(any(SendEmailRequest.class))).thenThrow(new EmailSendException("SendGrid rejected", 502));

		EmailSendException exception = assertThrows(EmailSendException.class, () -> service.send(buildRequest()));

		assertEquals(502, exception.getStatusCode());
		verify(repository).markFailed(anyString(), eq("SendGrid rejected"), any(Instant.class));
		verify(repository, never()).markSent(anyString(), any(), any(Instant.class));
	}

	@Test
	void swallowsBookkeepingFailureButKeepsOriginalError() {
		when(delegate.send(any(SendEmailRequest.class))).thenThrow(new EmailSendException("SendGrid rejected", 502));
		when(repository.markFailed(anyString(), anyString(), any(Instant.class)))
				.thenThrow(new RuntimeException("db down"));

		EmailSendException exception = assertThrows(EmailSendException.class, () -> service.send(buildRequest()));

		assertEquals(502, exception.getStatusCode());
	}
}
