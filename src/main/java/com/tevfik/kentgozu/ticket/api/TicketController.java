package com.tevfik.kentgozu.ticket.api;

import com.tevfik.kentgozu.ticket.api.dto.NearbyTicketResponse;
import com.tevfik.kentgozu.ticket.api.dto.TicketCreateRequest;
import com.tevfik.kentgozu.ticket.api.dto.TicketCreatedResponse;
import com.tevfik.kentgozu.ticket.domain.Ticket;
import com.tevfik.kentgozu.ticket.service.TicketCreationService;
import com.tevfik.kentgozu.ticket.spatial.TicketSpatialQueryService;
import jakarta.validation.Valid;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TicketController {

	private final TicketCreationService ticketCreationService;
	private final TicketSpatialQueryService ticketSpatialQueryService;
	private final GeometryFactory geometryFactory;

	public TicketController(
			TicketCreationService ticketCreationService,
			TicketSpatialQueryService ticketSpatialQueryService,
			GeometryFactory geometryFactory) {
		this.ticketCreationService = ticketCreationService;
		this.ticketSpatialQueryService = ticketSpatialQueryService;
		this.geometryFactory = geometryFactory;
	}

	@PostMapping("/api/tickets")
	public ResponseEntity<TicketCreatedResponse> createTicket(@Valid @RequestBody TicketCreateRequest request) {
		TicketCreatedResponse body = ticketCreationService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping("/api/tickets/nearby")
	public List<NearbyTicketResponse> nearby(
			@RequestParam("lat") double latitude,
			@RequestParam("lon") double longitude,
			@RequestParam("radius") double radiusCrs) {
		Point reference = geometryFactory.createPoint(new Coordinate(longitude, latitude));
		List<Ticket> tickets = ticketSpatialQueryService.findWithinCrsDistanceAsync(reference, radiusCrs).join();
		return tickets.stream().map(TicketController::toNearby).toList();
	}

	private static NearbyTicketResponse toNearby(Ticket t) {
		return new NearbyTicketResponse(
				t.getId(),
				t.getTitle(),
				t.getStatus().name(),
				t.getLocation().getX(),
				t.getLocation().getY(),
				t.getCreatedAt());
	}
}
