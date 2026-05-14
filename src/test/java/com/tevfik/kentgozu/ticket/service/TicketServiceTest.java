package com.tevfik.kentgozu.ticket.service;

import com.tevfik.kentgozu.KentgozuApplication;
import com.tevfik.kentgozu.ai_analysis.dto.VisionAnalysisResult;
import com.tevfik.kentgozu.ai_analysis.service.NlpEmbeddingService;
import com.tevfik.kentgozu.ai_analysis.service.VisionAnalysisService;
import com.tevfik.kentgozu.ticket.domain.Ticket;
import com.tevfik.kentgozu.ticket.domain.TicketStatus;
import com.tevfik.kentgozu.ticket.persistence.TicketRepository;
import com.tevfik.kentgozu.ticket.persistence.TicketSupportRepository;
import com.tevfik.kentgozu.user.domain.User;
import com.tevfik.kentgozu.user.persistence.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(
		classes = KentgozuApplication.class,
		properties = {
				"spring.autoconfigure.exclude="
						+ "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
						+ "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration,"
						+ "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
				"spring.jpa.hibernate.ddl-auto=update",
				"spring.ai.google.genai.api-key=test-key",
				"resilience4j.ratelimiter.instances.geminiRateLimiter.limitForPeriod=1000000",
				"resilience4j.ratelimiter.instances.geminiRateLimiter.limitRefreshPeriod=1s"
		})
@Transactional
class TicketServiceTest {

	private static final byte[] TINY_PNG = Base64.getDecoder().decode(
			"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

	@Autowired
	private TicketService ticketService;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private TicketSupportRepository ticketSupportRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private GeometryFactory geometryFactory;

	@MockitoBean
	private VisionAnalysisService visionAnalysisService;

	@MockitoBean
	private NlpEmbeddingService nlpEmbeddingService;

	@Test
	@DisplayName("Benzer konum+vektör: yeni ticket ID üretilmez, destekçi eklenir ve aciliyet güncellenir")
	void createOrMergeTicket_whenSimilarMatch_mergesWithoutNewTicketId() throws Exception {
		float[] sharedEmbedding = new float[768];
		Arrays.fill(sharedEmbedding, 0.031f);

		User reporter = userRepository.findByEmailIgnoreCase("admin@kentgozu.com").orElseThrow();
		User supporter = new User();
		supporter.setEmail("merge-supporter@kentgozu.com");
		supporter.setDisplayName("Destekçi");
		supporter.setPasswordHash(new BCryptPasswordEncoder().encode("x"));
		supporter = userRepository.saveAndFlush(supporter);

		Ticket existing = new Ticket();
		existing.setReporter(reporter);
		existing.setTitle("Mevcut çukur");
		existing.setDescription("Açıklama");
		existing.setStatus(TicketStatus.SUBMITTED);
		existing.setLocation(geometryFactory.createPoint(new Coordinate(28.9784, 41.0082)));
		existing.setEmbedding(sharedEmbedding);
		existing.setCategory("Altyapı");
		existing.setUrgencyScore(6);
		existing.setVisionSummary("Eski özet");
		existing = ticketRepository.saveAndFlush(existing);
		long existingId = existing.getId();
		long ticketCountBefore = ticketRepository.count();

		when(this.visionAnalysisService.analyze(any())).thenReturn(new VisionAnalysisResult("Çevre", 8, "Yeni özet"));
		when(this.nlpEmbeddingService.generateEmbedding(anyString())).thenReturn(sharedEmbedding);

		MockMultipartFile image = new MockMultipartFile("file", "x.png", "image/png", TINY_PNG);
		CreateOrMergeTicketCommand cmd = new CreateOrMergeTicketCommand(
				supporter.getEmail(),
				"Yeni ihbar",
				"Detay",
				28.9784,
				41.0082,
				image);

		CreateOrMergeOutcome outcome = this.ticketService.createOrMergeTicket(cmd);

		assertThat(outcome).isInstanceOf(CreateOrMergeOutcome.Merged.class);
		assertThat(((CreateOrMergeOutcome.Merged) outcome).ticketId()).isEqualTo(existingId);
		assertThat(this.ticketRepository.count()).isEqualTo(ticketCountBefore);

		Ticket merged = this.ticketRepository.findById(existingId).orElseThrow();
		assertThat(merged.getUrgencyScore()).isGreaterThan(6);
		assertThat(this.ticketSupportRepository.existsByTicketIdAndSupporterId(existingId, supporter.getId())).isTrue();
	}
}
