package com.tevfik.kentgozu.ticket.domain;

import com.tevfik.kentgozu.user.domain.User;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
		name = "ticket_supports",
		uniqueConstraints = @UniqueConstraint(name = "uk_ticket_support_ticket_user", columnNames = {"ticket_id", "supporter_id"}))
@Getter
@Setter
@NoArgsConstructor
public class TicketSupport {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "ticket_id", nullable = false)
	private Ticket ticket;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "supporter_id", nullable = false)
	private User supporter;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onPersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
