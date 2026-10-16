package com.serveless.envio.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.serveless.envio.dto.SendEmailRequest;
import com.serveless.envio.dto.SendEmailResponse;
import com.serveless.envio.exception.EmailSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SendGridEmailService implements EmailService {

	private static final Logger log = LoggerFactory.getLogger(SendGridEmailService.class);

	private final SendGrid sendGrid;
	private final String fromEmail;
	private final String fromName;

	public SendGridEmailService(
			SendGrid sendGrid,
			@Value("${sendgrid.from-email}") String fromEmail,
			@Value("${sendgrid.from-name}") String fromName) {
		this.sendGrid = sendGrid;
		this.fromEmail = fromEmail;
		this.fromName = fromName;
	}

	private String lastProviderMessageId;

	public String getLastProviderMessageId() {
		return lastProviderMessageId;
	}

	@Override
	public SendEmailResponse send(SendEmailRequest request) {
		lastProviderMessageId = null;
		Email from = new Email(fromEmail, fromName);
		Email to = new Email(request.getTo(), request.getToName());
		Content content = new Content("text/html", request.getMessage());
		Mail mail = new Mail(from, request.getSubject(), to, content);

		Request sgRequest = new Request();
		try {
			sgRequest.setMethod(Method.POST);
			sgRequest.setEndpoint("mail/send");
			sgRequest.setBody(mail.build());
		} catch (IOException ex) {
			throw new EmailSendException("Failed to build the SendGrid request", 500, ex);
		}

		Response response;
		try {
			response = sendGrid.api(sgRequest);
		} catch (IOException ex) {
			throw new EmailSendException("Failed to reach SendGrid", 502, ex);
		}

		int statusCode = response.getStatusCode();
		if (statusCode >= 400) {
			throw new EmailSendException("SendGrid rejected the request", statusCode);
		}

		try {
			if (response.getHeaders() != null) {
				for (String key : response.getHeaders().keySet()) {
					if (key != null && "X-Message-Id".equalsIgnoreCase(key)) {
						lastProviderMessageId = response.getHeaders().get(key);
						break;
					}
				}
			}
		} catch (Exception ignored) {
			lastProviderMessageId = null;
		}

		log.info("Email accepted by SendGrid for recipient {} with status {}", request.getTo(), statusCode);
		return SendEmailResponse.ok("Email accepted for delivery");
	}
}