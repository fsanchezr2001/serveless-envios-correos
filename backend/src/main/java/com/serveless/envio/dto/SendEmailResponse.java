package com.serveless.envio.dto;

public class SendEmailResponse {
	private boolean success;
	private String message;

	public SendEmailResponse() {
	}

	public SendEmailResponse(boolean success, String message) {
		this.success = success;
		this.message = message;
	}

	public static SendEmailResponse ok(String message) {
		return new SendEmailResponse(true, message);
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}