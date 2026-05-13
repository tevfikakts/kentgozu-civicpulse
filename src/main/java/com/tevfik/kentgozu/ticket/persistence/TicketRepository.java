package com.tevfik.kentgozu.ticket.persistence;

import com.tevfik.kentgozu.ticket.domain.Ticket;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	/**
	 * CRS mesafesi içindeki ihbarlar; PostGIS {@code ST_DWithin} (GiST uyumlu, {@code ST_Distance} yok).
	 * Yerel SQL ile Hibernate 6 SqmParser uzamsal JPQL ayrıştırma hatasından kaçınılır.
	 * {@code geometry(Point,4326)} için mesafe CRS birimindedir (derece).
	 */
	@Query(value = "SELECT * FROM tickets WHERE ST_DWithin(location, :reference, :distance)", nativeQuery = true)
	List<Ticket> findAllWithinCrsDistance(@Param("reference") Point reference, @Param("distance") double distance);
}
