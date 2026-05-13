package com.tevfik.kentgozu.security.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Faz 2: kimlik doğrulama isteği (şifre doğrulaması henüz yok; rol + e-posta ile JWT üretimi).
 */
public record LoginRequest(
		@NotBlank @Email String email,
		@NotBlank String role) {
}
