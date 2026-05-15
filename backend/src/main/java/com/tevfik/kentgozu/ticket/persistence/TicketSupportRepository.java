package com.tevfik.kentgozu.ticket.persistence;

import com.tevfik.kentgozu.ticket.domain.TicketSupport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketSupportRepository extends JpaRepository<TicketSupport, Long> {

	boolean existsByTicketIdAndSupporterId(Long ticketId, Long supporterId);

	long countByTicketId(Long ticketId);
}
