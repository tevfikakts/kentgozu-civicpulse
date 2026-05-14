package com.tevfik.kentgozu.ticket.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "ticket_gallery_images")
@Getter
@Setter
@NoArgsConstructor
public class TicketGalleryImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "ticket_id", nullable = false)
	private Ticket ticket;

	@JdbcTypeCode(SqlTypes.VARBINARY)
	@Column(nullable = false, columnDefinition = "bytea")
	private byte[] imageData;

	@Column(nullable = false, length = 128)
	private String contentType;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onPersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
