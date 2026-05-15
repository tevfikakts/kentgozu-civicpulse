package com.tevfik.kentgozu.ai_analysis.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * İhbar metinlerini Google GenAI embedding modeli ile vektöre dönüştürür.
 * Çıktı boyutu pgvector / Supabase sınırı gereği sabit 768 olmalıdır.
 */
@Service
public class NlpEmbeddingService {

	public static final int EMBEDDING_DIMENSIONS = 768;

	private final EmbeddingModel embeddingModel;

	public NlpEmbeddingService(EmbeddingModel embeddingModel) {
		this.embeddingModel = embeddingModel;
	}

	/**
	 * Verilen ihbar metninin anlamsal embedding vektörünü üretir (768 float).
	 */
	public float[] generateEmbedding(String text) {
		if (!StringUtils.hasText(text)) {
			throw new IllegalArgumentException("İhbar metni boş olamaz.");
		}
		float[] vector = this.embeddingModel.embed(text.trim());
		if (vector == null || vector.length != EMBEDDING_DIMENSIONS) {
			throw new IllegalStateException(
					"Beklenen embedding boyutu " + EMBEDDING_DIMENSIONS + " idi; model "
							+ (vector == null ? "null" : String.valueOf(vector.length)) + " döndürdü.");
		}
		return vector;
	}
}
