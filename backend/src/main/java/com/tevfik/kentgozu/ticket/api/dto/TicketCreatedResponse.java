package com.tevfik.kentgozu.ticket.api.dto;

import java.time.Instant;

/** Oluşturulan ihbarın dışa açık özeti. */
public record TicketCreatedResponse(
		Long id,
		String title,
		String description,
		String status,
		String reporterEmail,
		Double longitude,
		Double latitude,
		Instant createdAt) {
}
