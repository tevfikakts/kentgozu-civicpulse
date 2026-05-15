package com.tevfik.kentgozu.ticket.api.dto;

import java.time.Instant;

/** Yakındaki ihbar özeti (entity dışarı verilmez). */
public record NearbyTicketResponse(
		Long id,
		String title,
		String status,
		Double longitude,
		Double latitude,
		Instant createdAt) {
}
