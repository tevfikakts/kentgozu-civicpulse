package com.tevfik.kentgozu.ticket;

/**
 * ticket modülünün dışarıya yayınladığı domain event; realtime modülü dinler.
 * Alanlar {@code NearbyTicketResponse} ile uyumlu tutulmuştur.
 */
public record TicketCreatedEvent(
		long id,
		String title,
		String status,
		double latitude,
		double longitude,
		String reporterEmail,
		String category,
		int urgencyScore) {
}
