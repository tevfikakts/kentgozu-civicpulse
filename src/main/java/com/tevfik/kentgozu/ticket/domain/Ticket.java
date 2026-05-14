package com.tevfik.kentgozu.ticket.domain;

import com.tevfik.kentgozu.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

	/** WGS 84 (EPSG:4326) — konum {@link Point} tipinde ve bu SRID ile saklanır. */
	private static final int REQUIRED_LOCATION_SRID = 4326;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "reporter_id", nullable = false)
	private User reporter;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private TicketStatus status;

	@JdbcTypeCode(SqlTypes.GEOMETRY)
	@Column(nullable = false, columnDefinition = "geometry(Point,4326)")
	private Point location;

	/** pgvector {@code vector(768)} — metin gömme vektörü (ör. 768 boyutlu model çıktısı). */
	@JdbcTypeCode(SqlTypes.VECTOR)
	@Array(length = 768)
	@Column(columnDefinition = "vector(768)")
	private float[] embedding;

	@Column(length = 64)
	private String category;

	@Column(name = "urgency_score")
	private Integer urgencyScore;

	@Column(name = "vision_summary", columnDefinition = "text")
	private String visionSummary;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onPersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		Objects.requireNonNull(location, "Ticket.location");
		if (location.getSRID() != REQUIRED_LOCATION_SRID) {
			throw new IllegalStateException(
					"Ticket.location SRID must be " + REQUIRED_LOCATION_SRID + " (WGS 84); use the GeometryFactory bean.");
		}
	}
}
