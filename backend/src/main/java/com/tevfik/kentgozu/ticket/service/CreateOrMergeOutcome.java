package com.tevfik.kentgozu.ticket.service;

/** {@link TicketService#createOrMergeTicket} sonucu — yeni kayıt veya mevcut ihbara birleştirme. */
public sealed interface CreateOrMergeOutcome permits CreateOrMergeOutcome.Created, CreateOrMergeOutcome.Merged {

	record Created(long ticketId) implements CreateOrMergeOutcome {
	}

	record Merged(long ticketId, long supporterCount) implements CreateOrMergeOutcome {
	}
}
