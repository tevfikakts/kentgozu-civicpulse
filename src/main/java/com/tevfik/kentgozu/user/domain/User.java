package com.tevfik.kentgozu.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
		name = "app_users",
		indexes = @Index(name = "idx_app_users_email", columnList = "email", unique = true))
@Getter
@Setter
@NoArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	@Column(nullable = false, length = 120)
	private String displayName;

	@Column(nullable = false, length = 255)
	private String passwordHash;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void markCreatedAt() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
