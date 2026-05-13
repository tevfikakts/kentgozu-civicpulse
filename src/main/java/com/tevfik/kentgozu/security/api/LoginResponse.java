package com.tevfik.kentgozu.security.api;

/**
 * JWT erişim yanıtı (Bearer).
 */
public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
