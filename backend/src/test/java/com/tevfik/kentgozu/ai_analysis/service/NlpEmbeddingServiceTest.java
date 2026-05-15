package com.tevfik.kentgozu.ai_analysis.service;

import com.tevfik.kentgozu.KentgozuApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
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
				"spring.ai.google.genai.api-key=test-key"
		})
class NlpEmbeddingServiceTest {

	@Autowired
	private NlpEmbeddingService nlpEmbeddingService;

	@MockitoBean
	private EmbeddingModel embeddingModel;

	@BeforeEach
	void stubEmbedding() {
		when(this.embeddingModel.embed(anyString())).thenAnswer(invocation -> {
			float[] vector = new float[NlpEmbeddingService.EMBEDDING_DIMENSIONS];
			vector[0] = 0.042f;
			vector[767] = -0.017f;
			return vector;
		});
	}

	@Test
	@DisplayName("Örnek ihbar metni: vektör 768 boyutlu ve boş değil")
	void generateEmbedding_sampleReportText_returns768FloatVector() {
		String report = "Kadıköy'de ana cadde üzerinde derin çukur, gece yarısından beri işaretlenmemiş.";

		float[] vector = this.nlpEmbeddingService.generateEmbedding(report);

		assertThat(vector).isNotNull();
		assertThat(vector.length).isEqualTo(768);
		assertThat(vector.length).isGreaterThan(0);
	}
}
