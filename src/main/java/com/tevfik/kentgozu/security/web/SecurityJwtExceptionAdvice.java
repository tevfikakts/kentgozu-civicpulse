package com.tevfik.kentgozu.security.web;

import com.tevfik.kentgozu.security.jwt.ExpiredJwtException;
import com.tevfik.kentgozu.security.jwt.InvalidJwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * JWT doğrulama hataları; filtrede {@link org.springframework.web.servlet.HandlerExceptionResolver}
 * ile bu tavsiyelere yönlendirilir.
 */
@RestControllerAdvice
public class SecurityJwtExceptionAdvice {

	@ExceptionHandler(ExpiredJwtException.class)
	public ResponseEntity<ProblemDetail> expired(ExpiredJwtException ex, HttpServletRequest request) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
		detail.setTitle("JWT Expired");
		detail.setInstance(URI.create(request.getRequestURI()));
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(detail);
	}

	@ExceptionHandler(InvalidJwtException.class)
	public ResponseEntity<ProblemDetail> invalid(InvalidJwtException ex, HttpServletRequest request) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
		detail.setTitle("JWT Invalid");
		detail.setInstance(URI.create(request.getRequestURI()));
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(detail);
	}
}
