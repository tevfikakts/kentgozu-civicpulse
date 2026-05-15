package com.tevfik.kentgozu.ticket.service;

import com.tevfik.kentgozu.ticket.TicketCreatedEvent;
import com.tevfik.kentgozu.ticket.api.dto.TicketCreateRequest;
import com.tevfik.kentgozu.ticket.api.dto.TicketCreatedResponse;
import com.tevfik.kentgozu.ticket.domain.Ticket;
import com.tevfik.kentgozu.ticket.domain.TicketStatus;
import com.tevfik.kentgozu.ticket.persistence.TicketRepository;
import com.tevfik.kentgozu.user.domain.User;
import com.tevfik.kentgozu.user.persistence.UserRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class TicketCreationService {

	private final TicketRepository ticketRepository;
	private final UserRepository userRepository;
	private final GeometryFactory geometryFactory;
	private final ApplicationEventPublisher eventPublisher;

	public TicketCreationService(
			TicketRepository ticketRepository,
			UserRepository userRepository,
			GeometryFactory geometryFactory,
			ApplicationEventPublisher eventPublisher) {
		this.ticketRepository = ticketRepository;
		this.userRepository = userRepository;
		this.geometryFactory = geometryFactory;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public TicketCreatedResponse create(TicketCreateRequest request) {
		User reporter = userRepository.findByEmailIgnoreCase(request.reporterEmail().trim())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporter not found"));
		TicketStatus status = parseStatus(request.status());
		Point location = geometryFactory.createPoint(new Coordinate(request.longitude(), request.latitude()));

		Ticket ticket = new Ticket();
		ticket.setReporter(reporter);
		ticket.setTitle(request.title().trim());
		ticket.setDescription(request.description());
		ticket.setStatus(status);
		ticket.setLocation(location);
		ticket.setEmbedding(null);

		Ticket saved = ticketRepository.save(ticket);
		eventPublisher.publishEvent(new TicketCreatedEvent(
				saved.getId(),
				saved.getTitle(),
				saved.getStatus().name(),
				saved.getLocation().getY(),
				saved.getLocation().getX(),
				reporter.getEmail(),
				saved.getCategory() != null ? saved.getCategory() : "",
				saved.getUrgencyScore() != null ? saved.getUrgencyScore() : 0));
		return new TicketCreatedResponse(
				saved.getId(),
				saved.getTitle(),
				saved.getDescription(),
				saved.getStatus().name(),
				reporter.getEmail(),
				saved.getLocation().getX(),
				saved.getLocation().getY(),
				saved.getCreatedAt());
	}

	private static TicketStatus parseStatus(String raw) {
		String normalized = raw.trim().toUpperCase(Locale.ROOT);
		try {
			return TicketStatus.valueOf(normalized);
		}
		catch (IllegalArgumentException ex) {
			String allowed = Arrays.stream(TicketStatus.values())
					.map(Enum::name)
					.collect(Collectors.joining(", "));
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Invalid ticket status '" + raw.trim() + "'. Allowed values (case-insensitive): " + allowed + ".");
		}
	}
}
