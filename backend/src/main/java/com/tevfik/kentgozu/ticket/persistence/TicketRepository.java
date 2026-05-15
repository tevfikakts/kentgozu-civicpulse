package com.tevfik.kentgozu.ticket.persistence;

import com.tevfik.kentgozu.ticket.domain.Ticket;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	/**
	 * CRS mesafesi içindeki ihbarlar; PostGIS {@code ST_DWithin} (GiST uyumlu, {@code ST_Distance} yok).
	 * Yerel SQL ile Hibernate 6 SqmParser uzamsal JPQL ayrıştırma hatasından kaçınılır.
	 * {@code geometry(Point,4326)} için mesafe CRS birimindedir (derece).
	 */
	@Query(value = "SELECT * FROM tickets WHERE ST_DWithin(location, :reference, :distance)", nativeQuery = true)
	List<Ticket> findAllWithinCrsDistance(@Param("reference") Point reference, @Param("distance") double distance);

	/**
	 * Şelale: (1) {@code geography} + {@code ST_DWithin} ile 50 m ve aktif statüler; (2) pgvector kosinüs {@code <=>}
	 * ile {@code < 0.30} eşiği; en yakın tek kayıt.
	 * <p>
	 * Uzamsal tarafın metre cinsinden doğru ve GiST ile indekslenebilir olması için {@code location::geography}
	 * kullanılır — üretimde {@code CREATE INDEX ... ON tickets USING gist ((location::geography));} önerilir.
	 * </p>
	 */
	@Query(
			value = """
					WITH spatial_filtered AS (
						SELECT t.id AS tid, t.embedding AS emb
						FROM tickets t
						WHERE t.status IN ('OPEN', 'SUBMITTED', 'IN_PROGRESS')
							AND t.embedding IS NOT NULL
							AND ST_DWithin(
								t.location::geography,
								CAST(:location AS geometry)::geography,
								50.0
							)
					)
					SELECT sf.tid AS "ticketId",
						(sf.emb <=> CAST(:embedding AS vector(768))) AS "semanticDistance"
					FROM spatial_filtered sf
					WHERE (sf.emb <=> CAST(:embedding AS vector(768))) < 0.30
					ORDER BY sf.emb <=> CAST(:embedding AS vector(768))
					LIMIT 1
					""",
			nativeQuery = true)
	Optional<SimilarTicketResult> findSimilarTicket(@Param("location") Point location, @Param("embedding") float[] embedding);
}
