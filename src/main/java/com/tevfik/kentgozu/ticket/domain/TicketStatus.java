package com.tevfik.kentgozu.ticket.domain;

public enum TicketStatus {
	SUBMITTED,
	/** AI analizi kota / zaman aşımı sonrası yeniden işlenecek kayıtlar. */
	AI_PENDING,
	OPEN,
	IN_PROGRESS,
	RESOLVED,
	CLOSED
}
