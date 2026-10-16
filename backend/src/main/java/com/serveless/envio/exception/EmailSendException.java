package com.serveless.envio.exception;

public class EmailSendException extends RuntimeException {

	private final int statusCode;

	public EmailSendException(String message, int statusCode) {
		super(message);
		this.statusCode = statusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}
}