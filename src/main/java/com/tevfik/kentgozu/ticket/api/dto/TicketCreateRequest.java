package com.tevfik.kentgozu.ticket.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * İhbar oluşturma isteği (harici API); konum WGS 84 derece olarak iletilir, sunucu JTS {@code Point} üretir.
 */
public record TicketCreateRequest(
		@NotBlank @Email String reporterEmail,
		@NotBlank @Size(max = 200) String title,
		@NotNull @Size(max = 10_000) String description,
		@NotBlank String status,
		@NotNull Double longitude,
		@NotNull Double latitude) {
}
