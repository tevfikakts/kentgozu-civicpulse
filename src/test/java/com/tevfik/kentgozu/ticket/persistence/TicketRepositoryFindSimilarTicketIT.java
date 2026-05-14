package com.tevfik.kentgozu.ticket.persistence;

import com.tevfik.kentgozu.KentgozuApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
		classes = KentgozuApplication.class,
		properties = {
				"spring.autoconfigure.exclude="
						+ "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
						+ "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration,"
						+ "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
				"spring.jpa.hibernate.ddl-auto=update",
				"spring.ai.google.genai.api-key=test-key"
		})
@Transactional
class TicketRepositoryFindSimilarTicketIT {

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private GeometryFactory geometryFactory;

	@Test
	@DisplayName("findSimilarTicket: native sorgu PostgreSQL'de hatasız çalışır (eşleşme olmayabilir)")
	void findSimilarTicket_nativeQueryExecutes() {
		Point ref = geometryFactory.createPoint(new Coordinate(28.9784, 41.0082));
		float[] embedding = new float[768];
		Arrays.fill(embedding, 0.01f);

		Optional<SimilarTicketResult> result = ticketRepository.findSimilarTicket(ref, embedding);

		assertThat(result).isEmpty();
	}
}
