package com.tevfik.kentgozu.ticket.persistence;

import com.tevfik.kentgozu.ticket.domain.TicketGalleryImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketGalleryImageRepository extends JpaRepository<TicketGalleryImage, Long> {
}
