package com.serveless.envio.service;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.serveless.envio.dto.SendEmailRequest;
import com.serveless.envio.dto.SendEmailResponse;
import com.serveless.envio.exception.EmailSendException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SendGridEmailServiceTest {

	private static final String FROM_EMAIL = "no-reply@example.com";
	private static final String FROM_NAME = "Serveless Envio Correo";

	private SendEmailRequest buildRequest() {
		SendEmailRequest request = new SendEmailRequest();
		request.setTo("recipient@example.com");
		request.setToName("Recipient");
		request.setSubject("Test subject");
		request.setMessage("<p>Hello from test</p>");
		return request;
	}

	private Response responseWithStatus(int statusCode) {
		Response response = new Response();
		response.setStatusCode(statusCode);
		return response;
	}

	@Test
	void returnsSuccessWhenSendGridAcceptsTheMessage() throws Exception {
		SendGrid sendGrid = mock(SendGrid.class);
		when(sendGrid.api(any(Request.class))).thenReturn(responseWithStatus(202));

		SendGridEmailService service = new SendGridEmailService(sendGrid, FROM_EMAIL, FROM_NAME);

		SendEmailResponse response = service.send(buildRequest());

		assertTrue(response.isSuccess());
	}

	@Test
	void throwsEmailSendExceptionWhenSendGridRejectsTheMessage() throws Exception {
		SendGrid sendGrid = mock(SendGrid.class);
		when(sendGrid.api(any(Request.class))).thenReturn(responseWithStatus(400));

		SendGridEmailService service = new SendGridEmailService(sendGrid, FROM_EMAIL, FROM_NAME);

		EmailSendException exception = assertThrows(EmailSendException.class, () -> service.send(buildRequest()));
		assertEquals(400, exception.getStatusCode());
	}
}