package com.tevfik.kentgozu.ticket.api;

import com.tevfik.kentgozu.ticket.api.dto.NearbyTicketResponse;
import com.tevfik.kentgozu.ticket.api.dto.TicketCreateRequest;
import com.tevfik.kentgozu.ticket.api.dto.TicketCreatedResponse;
import com.tevfik.kentgozu.ticket.domain.Ticket;
import com.tevfik.kentgozu.ticket.service.CreateOrMergeOutcome;
import com.tevfik.kentgozu.ticket.service.CreateOrMergeTicketCommand;
import com.tevfik.kentgozu.ticket.service.TicketCreationService;
import com.tevfik.kentgozu.ticket.service.TicketService;
import com.tevfik.kentgozu.ticket.spatial.TicketSpatialQueryService;
import jakarta.validation.Valid;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class TicketController {

	private static final String DEFAULT_REPORTER_EMAIL = "admin@kentgozu.com";

	private final TicketCreationService ticketCreationService;
	private final TicketService ticketService;
	private final TicketSpatialQueryService ticketSpatialQueryService;
	private final GeometryFactory geometryFactory;

	public TicketController(
			TicketCreationService ticketCreationService,
			TicketService ticketService,
			TicketSpatialQueryService ticketSpatialQueryService,
			GeometryFactory geometryFactory) {
		this.ticketCreationService = ticketCreationService;
		this.ticketService = ticketService;
		this.ticketSpatialQueryService = ticketSpatialQueryService;
		this.geometryFactory = geometryFactory;
	}

	@PostMapping("/api/tickets")
	public ResponseEntity<TicketCreatedResponse> createTicket(@Valid @RequestBody TicketCreateRequest request) {
		TicketCreatedResponse body = ticketCreationService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@PostMapping(value = "/api/tickets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CreateOrMergeOutcome> createOrMergeTicket(
			@RequestParam("title") String title,
			@RequestParam("description") String description,
			@RequestParam("latitude") double latitude,
			@RequestParam("longitude") double longitude,
			@RequestParam("file") MultipartFile file) throws IOException {
		CreateOrMergeTicketCommand command = new CreateOrMergeTicketCommand(
				DEFAULT_REPORTER_EMAIL,
				title,
				description,
				longitude,
				latitude,
				file);
		CreateOrMergeOutcome body = ticketService.createOrMergeTicket(command);
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
