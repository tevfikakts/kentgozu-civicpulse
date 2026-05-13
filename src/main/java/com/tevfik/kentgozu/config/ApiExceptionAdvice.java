package com.tevfik.kentgozu.config;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionAdvice {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
		detail.setTitle("Validation failed");
		detail.setInstance(URI.create(request.getRequestURI()));
		List<Map<String, String>> violations = ex.getBindingResult().getFieldErrors().stream()
				.map(ApiExceptionAdvice::fieldErrorToMap)
				.toList();
		detail.setProperty("errorType", "validation");
		detail.setProperty("violations", violations);
		return ResponseEntity.badRequest().body(detail);
	}

	private static Map<String, String> fieldErrorToMap(FieldError fe) {
		Map<String, String> m = new LinkedHashMap<>(3);
		m.put("field", fe.getField());
		m.put("message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "");
		if (fe.getRejectedValue() != null) {
			m.put("rejected", String.valueOf(fe.getRejectedValue()));
		}
		return m;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> notReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
		Throwable root = ex.getMostSpecificCause();
		String title;
		String detailMsg;
		String errorType;
		if (root instanceof JsonParseException) {
			title = "Malformed JSON";
			detailMsg = "Request body is not valid JSON.";
			errorType = "json_parse";
		}
		else if (root instanceof MismatchedInputException) {
			title = "Invalid JSON structure";
			detailMsg = root.getMessage() != null ? root.getMessage() : "JSON value does not match expected type or structure.";
			errorType = "json_structure";
		}
		else if (root instanceof InvalidFormatException ife) {
			title = "Invalid JSON value format";
			detailMsg = ife.getOriginalMessage() != null ? ife.getOriginalMessage() : "Cannot deserialize value.";
			errorType = "json_format";
		}
		else if (root instanceof JsonMappingException jme) {
			title = "JSON mapping error";
			detailMsg = jme.getOriginalMessage() != null ? jme.getOriginalMessage() : "Cannot map JSON to request type.";
			errorType = "json_mapping";
		}
		else {
			title = "Unreadable request body";
			detailMsg = root.getMessage() != null ? root.getMessage() : "Could not read HTTP message.";
			errorType = "message_not_readable";
		}
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detailMsg);
		detail.setTitle(title);
		detail.setInstance(URI.create(request.getRequestURI()));
		detail.setProperty("errorType", errorType);
		return ResponseEntity.badRequest().body(detail);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ProblemDetail> responseStatus(ResponseStatusException ex, HttpServletRequest request) {
		HttpStatusCode statusCode = ex.getStatusCode();
		HttpStatus status = HttpStatus.resolve(statusCode.value());
		if (status == null) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		String reason = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, reason);
		detail.setTitle(status.is4xxClientError() ? "Client error" : "Server error");
		detail.setInstance(URI.create(request.getRequestURI()));
		detail.setProperty("errorType", "response_status");
		return ResponseEntity.status(status).body(detail);
	}
}
