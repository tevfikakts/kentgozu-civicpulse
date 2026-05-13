package com.tevfik.kentgozu.security.jwt;

import org.springframework.security.authentication.BadCredentialsException;

/** İmza, yayıncı veya biçim geçersiz. */
public class InvalidJwtException extends BadCredentialsException {

	public InvalidJwtException(String message) {
		super(message);
	}

	public InvalidJwtException(String message, Throwable cause) {
		super(message, cause);
	}
}
