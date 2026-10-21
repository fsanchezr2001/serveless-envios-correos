package com.serveless.envio.exception;

import com.serveless.envio.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		List<String> details = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.toList();
		ErrorResponse body = new ErrorResponse(
				Instant.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"Validation failed",
				details);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(EmailSendException.class)
	public ResponseEntity<ErrorResponse> handleEmailSend(EmailSendException ex) {
		log.error("Email send failed with SendGrid status {}", ex.getStatusCode(), ex);
		ErrorResponse body = new ErrorResponse(
				Instant.now(),
				HttpStatus.BAD_GATEWAY.value(),
				HttpStatus.BAD_GATEWAY.getReasonPhrase(),
				ex.getMessage(),
				List.of());
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleMissingBody(HttpMessageNotReadableException ex) {
		log.error("Missing or malformed request body", ex);
		ErrorResponse body = new ErrorResponse(
				Instant.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"Request body is required or malformed",
				List.of());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
		log.error("Unexpected error processing request", ex);
		ErrorResponse body = new ErrorResponse(
				Instant.now(),
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
				"An unexpected error occurred",
				List.of());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
	}
}