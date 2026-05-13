package com.tevfik.kentgozu.ticket.spatial;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tevfik.kentgozu.ticket.domain.Ticket;
import com.tevfik.kentgozu.ticket.persistence.TicketRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * PostGIS ağır uzamsal sorguları: {@link CircuitBreaker} + {@link TimeLimiter} ile korunur;
 * açık devre / zaman aşımı / Redis hatasında önbellek veya boş liste ile kademeli bozulma.
 */
@Service
public class TicketSpatialQueryService {

	public static final String RESILIENCE_NAME = "postgisSpatial";

	private static final String REDIS_KEY_PREFIX = "kentgozu:geo:tickets:st_dwithin:v1:";
	private static final Duration CACHE_TTL = Duration.ofMinutes(10);

	private final TicketRepository ticketRepository;
	private final ObjectMapper objectMapper;
	private final ObjectProvider<StringRedisTemplate> redisTemplate;
	private final Executor spatialQueryExecutor;

	public TicketSpatialQueryService(
			TicketRepository ticketRepository,
			ObjectMapper objectMapper,
			ObjectProvider<StringRedisTemplate> redisTemplate,
			@Qualifier("spatialQueryExecutor") Executor spatialQueryExecutor) {
		this.ticketRepository = ticketRepository;
		this.objectMapper = objectMapper;
		this.redisTemplate = redisTemplate;
		this.spatialQueryExecutor = spatialQueryExecutor;
	}

	@CircuitBreaker(name = RESILIENCE_NAME, fallbackMethod = "findWithinCrsDistanceFallback")
	@TimeLimiter(name = RESILIENCE_NAME)
	public CompletableFuture<List<Ticket>> findWithinCrsDistanceAsync(Point reference, double distanceCrs) {
		return CompletableFuture.supplyAsync(() -> {
			List<Ticket> fresh = ticketRepository.findAllWithinCrsDistance(reference, distanceCrs);
			cacheTicketIds(reference, distanceCrs, fresh);
			return fresh;
		}, spatialQueryExecutor);
	}

	@SuppressWarnings("unused")
	private CompletableFuture<List<Ticket>> findWithinCrsDistanceFallback(
			Point reference, double distanceCrs, Throwable cause) {
		List<Ticket> cached = loadTicketsFromRedisOrEmpty(reference, distanceCrs);
		return CompletableFuture.completedFuture(cached);
	}

	private void cacheTicketIds(Point reference, double distanceCrs, List<Ticket> tickets) {
		StringRedisTemplate redis = redisTemplate.getIfAvailable();
		if (redis == null || tickets == null) {
			return;
		}
		try {
			List<Long> ids = tickets.stream().map(Ticket::getId).toList();
			String json = objectMapper.writeValueAsString(ids);
			redis.opsForValue().set(cacheKey(reference, distanceCrs), json, CACHE_TTL);
		}
		catch (Exception ignored) {
			// önbellek yazılamazsa sessizce yut (SRE: ana yol başarılı)
		}
	}

	private List<Ticket> loadTicketsFromRedisOrEmpty(Point reference, double distanceCrs) {
		StringRedisTemplate redis = redisTemplate.getIfAvailable();
		if (redis == null) {
			return Collections.emptyList();
		}
		try {
			String json = redis.opsForValue().get(cacheKey(reference, distanceCrs));
			if (json == null || json.isBlank()) {
				return Collections.emptyList();
			}
			List<Long> ids = objectMapper.readValue(json, new TypeReference<>() {
			});
			if (ids.isEmpty()) {
				return Collections.emptyList();
			}
			return ticketRepository.findAllById(ids);
		}
		catch (Exception e) {
			return Collections.emptyList();
		}
	}

	private static String cacheKey(Point reference, double distanceCrs) {
		String raw = reference.getX() + ":" + reference.getY() + ":" + reference.getSRID() + ":" + distanceCrs;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			return REDIS_KEY_PREFIX + HexFormat.of().formatHex(digest);
		}
		catch (Exception e) {
			return REDIS_KEY_PREFIX + Integer.toHexString(raw.hashCode());
		}
	}
}
