package com.tevfik.kentgozu.security.jwt;

import org.springframework.security.authentication.CredentialsExpiredException;

/**
 * JWT {@code exp} talebi geçmiş; filtre zincirinde yakalanıp {@code HandlerExceptionResolver} ile
 * {@code @RestControllerAdvice} yoluna yönlendirilebilir.
 */
public class ExpiredJwtException extends CredentialsExpiredException {

	public ExpiredJwtException(String message) {
		super(message);
	}
}
