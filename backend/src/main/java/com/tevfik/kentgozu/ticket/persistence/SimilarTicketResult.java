package com.tevfik.kentgozu.ticket.persistence;

/**
 * {@link TicketRepository#findSimilarTicket} için arayüz projeksiyonu — tam {@code Ticket} entity'si yüklenmez.
 */
public interface SimilarTicketResult {

	Long getTicketId();

	Double getSemanticDistance();
}
