package com.tevfik.kentgozu.ai_analysis.service;

import com.tevfik.kentgozu.ai_analysis.dto.VisionAnalysisResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
public class VisionAnalysisService {

	private static final Set<String> ALLOWED_CATEGORIES = Set.of("Altyapı", "Çevre", "Güvenlik");

	private static final String SYSTEM_PROMPT = """
			Sen bir kent bildirimi görsel analiz asistanısın.
			Görevin: yüklenen fotoğraftaki sorunu sınıflandırmak, aciliyet vermek ve çok kısa özet yazmaktır.
			Kurallar:
			- kategori alanı YALNIZCA şu üç değerden biri olabilir: Altyapı, Çevre, Güvenlik (tam eşleşme, tırnak veya ek sözcük yok).
			- aciliyet 1 ile 10 arasında tam sayıdır (10 en kritik; örn. ana su borusu patlaması, can güvenliği riski yüksekse 8-10).
			- kisa_ozet düz metin olmalıdır: markdown yok (liste, başlık #, kalın **, kod bloğu ``` vb. kullanma).
			- Yanıtı yalnızca istenen JSON şemasına uygun üret; ek açıklama veya biçimlendirme ekleme.
			""";

	private static final String USER_INSTRUCTION = """
			Bu görseldeki kentsel sorunu değerlendir ve şemaya uygun JSON çıktısı üret.
			""";

	private final ChatClient chatClient;
	private final StructuredOutputConverter<VisionAnalysisResult> visionAnalysisConverter;

	public VisionAnalysisService(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
		this.visionAnalysisConverter = new BeanOutputConverter<>(VisionAnalysisResult.class);
	}

	/**
	 * multipart/form-data ile yüklenen görseli Gemini 2.5 Flash ile analiz eder.
	 */
	public VisionAnalysisResult analyze(MultipartFile image) throws IOException {
		if (image == null || image.isEmpty()) {
			throw new IllegalArgumentException("Görsel dosyası zorunludur.");
		}

		Resource mediaResource = new ByteArrayResource(image.getBytes()) {
			@Override
			public String getFilename() {
				return StringUtils.hasText(image.getOriginalFilename()) ? image.getOriginalFilename() : "upload";
			}
		};
		MimeType mimeType = resolveMimeType(image);

		VisionAnalysisResult result = this.chatClient.prompt()
				.options(GoogleGenAiChatOptions.builder()
						.model("gemini-2.5-flash")
						.temperature(0.0)
						.build())
				.system(SYSTEM_PROMPT)
				.user(u -> u.text(USER_INSTRUCTION)
						.media(mimeType, mediaResource))
				.call()
				.entity(this.visionAnalysisConverter);

		validate(result);
		return result;
	}

	private static MimeType resolveMimeType(MultipartFile image) {
		String contentType = image.getContentType();
		if (!StringUtils.hasText(contentType)) {
			return MimeTypeUtils.IMAGE_JPEG;
		}
		return MimeType.valueOf(contentType);
	}

	private static void validate(VisionAnalysisResult result) {
		if (!ALLOWED_CATEGORIES.contains(result.kategori())) {
			throw new IllegalStateException("Model geçersiz kategori döndürdü: " + result.kategori());
		}
		if (result.aciliyet() < 1 || result.aciliyet() > 10) {
			throw new IllegalStateException("Aciliyet 1-10 aralığında olmalıydı: " + result.aciliyet());
		}
	}
}
