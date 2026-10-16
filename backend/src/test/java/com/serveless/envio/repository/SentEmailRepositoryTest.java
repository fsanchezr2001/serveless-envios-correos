package com.serveless.envio.repository;

import com.serveless.envio.model.EmailLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class SentEmailRepositoryTest {

	private static final Instant SENT_AT = Instant.parse("2026-01-01T00:00:00Z");

	@Autowired
	private EmailLogRepository repository;

	@Autowired
	private TestEntityManager entityManager;

	private String persistPending() {
		EmailLog email = new EmailLog();
		email.setTo("recipient@example.com");
		email.setToName("Recipient");
		email.setSubject("Test subject");
		email.setBody("<p>Hello</p>");
		email.setStatus("PENDING");
		email.setSentAt(Instant.parse("2025-12-31T00:00:00Z"));
		entityManager.persistAndFlush(email);
		return email.getId();
	}

	@Test
	void markSentTransitionsRowToSent() {
		String id = persistPending();

		repository.markSent(id, "msg-123", SENT_AT);
		entityManager.flush();
		entityManager.clear();

		EmailLog reloaded = repository.findById(id).orElseThrow();
		assertEquals("SENT", reloaded.getStatus());
		assertEquals("msg-123", reloaded.getProviderMessageId());
		assertEquals(SENT_AT, reloaded.getSentAt());
		assertNull(reloaded.getErrorMessage());
	}

	@Test
	void markFailedTransitionsRowToFailedAndRecordsError() {
		String id = persistPending();

		repository.markFailed(id, "SendGrid rejected", SENT_AT);
		entityManager.flush();
		entityManager.clear();

		EmailLog reloaded = repository.findById(id).orElseThrow();
		assertEquals("FAILED", reloaded.getStatus());
		assertEquals("SendGrid rejected", reloaded.getErrorMessage());
		assertEquals(SENT_AT, reloaded.getSentAt());
		assertNull(reloaded.getProviderMessageId());
	}
}
