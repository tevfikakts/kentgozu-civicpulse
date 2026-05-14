package com.tevfik.kentgozu.ai_analysis.dto;

/** Görsel analizi yapılandırılmış çıktısı (kategori, aciliyet, kısa özet). */
public record VisionAnalysisResult(String kategori, int aciliyet, String kisa_ozet) {
}
