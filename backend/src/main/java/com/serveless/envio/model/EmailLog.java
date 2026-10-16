package com.serveless.envio.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_logs")
public class EmailLog {

	@Id
	@Column(name = "id", length = 36, nullable = false)
	private String id = UUID.randomUUID().toString();

	@Column(name = "recipient", nullable = false, length = 255)
	private String to;

	@Column(name = "to_name", length = 255)
	private String toName;

	@Column(name = "subject", nullable = false, length = 150)
	private String subject;

	@Column(name = "body", nullable = false, columnDefinition = "TEXT")
	private String body;

	@Column(name = "status", nullable = false, length = 20)
	private String status;

	@Column(name = "sent_at", nullable = false)
	private Instant sentAt;

	@Column(name = "provider_message_id", length = 255)
	private String providerMessageId;

	@Column(name = "error_message", length = 2000)
	private String errorMessage;

	public EmailLog() {
		this.sentAt = Instant.now();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getToName() {
		return toName;
	}

	public void setToName(String toName) {
		this.toName = toName;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getSentAt() {
		return sentAt;
	}

	public void setSentAt(Instant sentAt) {
		this.sentAt = sentAt;
	}

	public String getProviderMessageId() {
		return providerMessageId;
	}

	public void setProviderMessageId(String providerMessageId) {
		this.providerMessageId = providerMessageId;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}
