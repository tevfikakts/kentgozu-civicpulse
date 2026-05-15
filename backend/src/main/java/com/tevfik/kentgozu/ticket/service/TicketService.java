package com.tevfik.kentgozu.ticket.service;

import com.tevfik.kentgozu.ai_analysis.dto.VisionAnalysisResult;
import com.tevfik.kentgozu.ai_analysis.service.NlpEmbeddingService;
import com.tevfik.kentgozu.ai_analysis.service.VisionAnalysisService;
import com.tevfik.kentgozu.ticket.TicketCreatedEvent;
import com.tevfik.kentgozu.ticket.domain.Ticket;
import com.tevfik.kentgozu.ticket.domain.TicketGalleryImage;
import com.tevfik.kentgozu.ticket.domain.TicketStatus;
import com.tevfik.kentgozu.ticket.domain.TicketSupport;
import com.tevfik.kentgozu.ticket.persistence.SimilarTicketResult;
import com.tevfik.kentgozu.ticket.persistence.TicketGalleryImageRepository;
import com.tevfik.kentgozu.ticket.persistence.TicketRepository;
import com.tevfik.kentgozu.ticket.persistence.TicketSupportRepository;
import com.tevfik.kentgozu.user.domain.User;
import com.tevfik.kentgozu.user.persistence.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Service
public class TicketService {

	static final int DEFAULT_FALLBACK_URGENCY = 5;
	static final String FALLBACK_CATEGORY = "Belirsiz";

	private final VisionAnalysisService visionAnalysisService;
	private final NlpEmbeddingService nlpEmbeddingService;
	private final TicketRepository ticketRepository;
	private final TicketSupportRepository ticketSupportRepository;
	private final TicketGalleryImageRepository ticketGalleryImageRepository;
	private final UserRepository userRepository;
	private final GeometryFactory geometryFactory;
	private final ApplicationEventPublisher eventPublisher;

	public TicketService(
			VisionAnalysisService visionAnalysisService,
			NlpEmbeddingService nlpEmbeddingService,
			TicketRepository ticketRepository,
			TicketSupportRepository ticketSupportRepository,
			TicketGalleryImageRepository ticketGalleryImageRepository,
			UserRepository userRepository,
			GeometryFactory geometryFactory,
			ApplicationEventPublisher eventPublisher) {
		this.visionAnalysisService = visionAnalysisService;
		this.nlpEmbeddingService = nlpEmbeddingService;
		this.ticketRepository = ticketRepository;
		this.ticketSupportRepository = ticketSupportRepository;
		this.ticketGalleryImageRepository = ticketGalleryImageRepository;
		this.userRepository = userRepository;
		this.geometryFactory = geometryFactory;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * Görsel + metin AI analizi, embedding ve şelale benzerlik; gerekirse mevcut ihbara birleştirir.
	 * Gemini RPM sınırı {@link RateLimiter} ile korunur; kota / zaman aşımında {@link #fallbackCreateTicket} veya
	 * içsel kademeli bozulma ile {@link TicketStatus#AI_PENDING} kayıt oluşturulur.
	 */
	@Transactional
	@RateLimiter(name = "geminiRateLimiter", fallbackMethod = "fallbackCreateTicket")
	public CreateOrMergeOutcome createOrMergeTicket(CreateOrMergeTicketCommand command) throws IOException {
		try {
			VisionAnalysisResult vision = this.visionAnalysisService.analyze(command.image());
			float[] embedding = this.nlpEmbeddingService.generateEmbedding(textForEmbedding(command));
			return continueAfterAi(command, vision, embedding);
		}
		catch (RuntimeException ex) {
			if (isQuotaTimeoutOrRateLimit(ex)) {
				return persistAiPending(command);
			}
			throw ex;
		}
	}

	@Transactional
	public CreateOrMergeOutcome fallbackCreateTicket(CreateOrMergeTicketCommand command, Throwable throwable) {
		return persistAiPending(command);
	}

	private CreateOrMergeOutcome continueAfterAi(CreateOrMergeTicketCommand command, VisionAnalysisResult vision, float[] embedding) {
		User reporter = loadReporter(command.reporterEmail());
		Point location = this.geometryFactory.createPoint(new Coordinate(command.longitude(), command.latitude()));
		Optional<SimilarTicketResult> similar = this.ticketRepository.findSimilarTicket(location, embedding);
		if (similar.isPresent()) {
			return mergeTicket(similar.get().getTicketId(), reporter, command, vision);
		}
		return createNewTicket(reporter, command, vision, embedding, location);
	}

	private User loadReporter(String email) {
		return this.userRepository.findByEmailIgnoreCase(email.trim())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporter not found"));
	}

	private CreateOrMergeOutcome persistAiPending(CreateOrMergeTicketCommand command) {
		User reporter = loadReporter(command.reporterEmail());
		Point location = this.geometryFactory.createPoint(new Coordinate(command.longitude(), command.latitude()));
		Ticket t = new Ticket();
		t.setReporter(reporter);
		t.setTitle(command.title().strip());
		t.setDescription(command.description());
		t.setStatus(TicketStatus.AI_PENDING);
		t.setLocation(location);
		t.setEmbedding(null);
		t.setCategory(FALLBACK_CATEGORY);
		t.setUrgencyScore(DEFAULT_FALLBACK_URGENCY);
		t.setVisionSummary(null);
		Ticket saved = this.ticketRepository.save(t);
		saveGalleryIfPresent(saved, command.image());
		eventPublisher.publishEvent(toCreatedEvent(saved));
		return new CreateOrMergeOutcome.Created(saved.getId());
	}

	private CreateOrMergeOutcome createNewTicket(
			User reporter,
			CreateOrMergeTicketCommand command,
			VisionAnalysisResult vision,
			float[] embedding,
			Point location) {
		Ticket t = new Ticket();
		t.setReporter(reporter);
		t.setTitle(command.title().strip());
		t.setDescription(command.description());
		t.setStatus(TicketStatus.SUBMITTED);
		t.setLocation(location);
		t.setEmbedding(embedding);
		t.setCategory(vision.kategori());
		t.setUrgencyScore(vision.aciliyet());
		t.setVisionSummary(vision.kisa_ozet());
		Ticket saved = this.ticketRepository.save(t);
		saveGalleryIfPresent(saved, command.image());
		eventPublisher.publishEvent(toCreatedEvent(saved));
		return new CreateOrMergeOutcome.Created(saved.getId());
	}

	private CreateOrMergeOutcome mergeTicket(
			long ticketId,
			User supporter,
			CreateOrMergeTicketCommand command,
			VisionAnalysisResult vision) {
		Ticket ticket = this.ticketRepository.findById(ticketId)
				.orElseThrow(() -> new IllegalStateException("findSimilarTicket ile dönen kayıt bulunamadı: " + ticketId));
		addSupporterIfEligible(ticket, supporter);
		bumpUrgency(ticket, vision.aciliyet());
		this.ticketRepository.save(ticket);
		saveGalleryIfPresent(ticket, command.image());
		eventPublisher.publishEvent(toCreatedEvent(ticket));
		long supporterCount = this.ticketSupportRepository.countByTicketId(ticket.getId());
		return new CreateOrMergeOutcome.Merged(ticket.getId(), supporterCount);
	}

	private void addSupporterIfEligible(Ticket ticket, User supporter) {
		if (supporter.getId().equals(ticket.getReporter().getId())) {
			return;
		}
		if (this.ticketSupportRepository.existsByTicketIdAndSupporterId(ticket.getId(), supporter.getId())) {
			return;
		}
		TicketSupport row = new TicketSupport();
		row.setTicket(ticket);
		row.setSupporter(supporter);
		this.ticketSupportRepository.save(row);
	}

	/** Mevcut ve gelen aciliyetten doğrusal + logaritmik artış; üst sınır 10. */
	private static void bumpUrgency(Ticket ticket, int incomingUrgency) {
		int current = ticket.getUrgencyScore() != null ? ticket.getUrgencyScore() : 5;
		int logBump = (int) Math.ceil(Math.log1p(incomingUrgency));
		int merged = Math.min(10, Math.max(current, incomingUrgency) + Math.min(2, Math.max(1, logBump)));
		ticket.setUrgencyScore(merged);
	}

	private void saveGalleryIfPresent(Ticket ticket, MultipartFile image) {
		if (image == null || image.isEmpty()) {
			return;
		}
		try {
			TicketGalleryImage g = new TicketGalleryImage();
			g.setTicket(ticket);
			g.setImageData(image.getBytes());
			g.setContentType(StringUtils.hasText(image.getContentType()) ? image.getContentType() : "application/octet-stream");
			this.ticketGalleryImageRepository.save(g);
		}
		catch (IOException ignored) {
			// Galeri eklenemezse ana ticket yine de geçerli kalsın
		}
	}

	private static TicketCreatedEvent toCreatedEvent(Ticket t) {
		return new TicketCreatedEvent(
				t.getId(),
				t.getTitle(),
				t.getStatus() != null ? t.getStatus().name() : "SUBMITTED",
				t.getLocation() != null ? t.getLocation().getY() : 0.0,
				t.getLocation() != null ? t.getLocation().getX() : 0.0,
				t.getReporter() != null ? t.getReporter().getEmail() : "",
				t.getCategory() != null ? t.getCategory() : "",
				t.getUrgencyScore() != null ? t.getUrgencyScore() : 0);
	}

	private static String textForEmbedding(CreateOrMergeTicketCommand command) {
		return command.title().strip() + "\n" + command.description();
	}

	private static boolean isQuotaTimeoutOrRateLimit(Throwable ex) {
		if (ex instanceof RequestNotPermitted) {
			return true;
		}
		for (Throwable t = ex; t != null; t = t.getCause()) {
			if (t instanceof TimeoutException) {
				return true;
			}
			if (t instanceof RestClientResponseException rce && rce.getStatusCode().value() == 429) {
				return true;
			}
			String msg = t.getMessage();
			if (msg != null && (msg.contains("429")
					|| msg.contains("RESOURCE_EXHAUSTED")
					|| msg.toLowerCase().contains("timeout")
					|| msg.contains("timed out"))) {
				return true;
			}
		}
		return false;
	}
}
