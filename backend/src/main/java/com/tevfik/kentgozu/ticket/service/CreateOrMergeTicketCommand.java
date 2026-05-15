package com.tevfik.kentgozu.ticket.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * AI destekli ihbar oluşturma / birleştirme girdisi.
 */
public record CreateOrMergeTicketCommand(
		String reporterEmail,
		String title,
		String description,
		double longitude,
		double latitude,
		MultipartFile image) {
}
