package com.tevfik.kentgozu.ai_analysis.service;

import com.tevfik.kentgozu.KentgozuApplication;
import com.tevfik.kentgozu.ai_analysis.dto.VisionAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
class VisionAnalysisServiceTest {

	/** 1x1 şeffaf PNG — sahte / yer tutucu görsel. */
	private static final byte[] MOCK_BURST_PIPE_IMAGE_BYTES = Base64.getDecoder().decode(
			"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

	private static final String MODEL_JSON = """
			{"kategori":"Altyapı","aciliyet":9,"kisa_ozet":"Yol üstünde ana su hattında patlama; yoğun su çıkışı ve çevrede taşkın riski gözleniyor."}
			""";

	@Autowired
	private VisionAnalysisService visionAnalysisService;

	@MockitoBean
	private ChatModel chatModel;

	@BeforeEach
	void stubGeminiResponse() {
		when(this.chatModel.call(any(Prompt.class))).thenAnswer(invocation -> new ChatResponse(
				List.of(new Generation(new AssistantMessage(MODEL_JSON.strip())))));
	}

	@Test
	@DisplayName("Sahte patlak boru görseli: aciliyet 8-10 ve markdown içermeyen düz metin özet")
	void analyze_withMockBurstPipeImage_returnsCriticalUrgencyAndPlainText() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"mock-burst-pipe.png",
				"image/png",
				MOCK_BURST_PIPE_IMAGE_BYTES);

		VisionAnalysisResult result = this.visionAnalysisService.analyze(file);

		assertThat(result.aciliyet()).isBetween(8, 10);
		assertThat(result.kategori()).isEqualTo("Altyapı");

		assertNoMarkdown(result.kategori());
		assertNoMarkdown(result.kisa_ozet());
	}

	private static void assertNoMarkdown(String text) {
		assertThat(text).as("kod çiti").doesNotContain("```");
		assertThat(text).as("markdown başlık").doesNotMatch("(?s).*^[ \\t]*#{1,6}\\s+\\S+.*");
		assertThat(text).as("kalın vurgu").doesNotContain("**");
		assertThat(text).as("alt çizgi vurgu").doesNotMatch(".*(^|[^\\w])__[^_].*");
		assertThat(text).as("liste madde").doesNotMatch("(?m)^[ \\t]*([-*+]|[0-9]+\\.)\\s+\\S+.*");
	}
}
