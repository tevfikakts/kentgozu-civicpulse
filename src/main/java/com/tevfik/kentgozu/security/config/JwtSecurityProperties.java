package com.tevfik.kentgozu.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * JWT imzalama ve doğrulama parametreleri — gizli anahtar yalnızca yapılandırmadan gelir.
 */
@Validated
@ConfigurationProperties(prefix = "kentgozu.security.jwt")
public class JwtSecurityProperties {

	/**
	 * HMAC-SHA256 için bayt dizisi; üretimde güçlü ve rastgele bir değer (ör. ortam değişkeni) kullanın.
	 */
	private String secret = "";

	private String issuer = "kentgozu";

	private long accessTokenValiditySeconds = 3600;

	private final List<String> publicPaths = new ArrayList<>(List.of("/api/v1/auth/**", "/api/auth/**", "/error"));

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public long getAccessTokenValiditySeconds() {
		return accessTokenValiditySeconds;
	}

	public void setAccessTokenValiditySeconds(long accessTokenValiditySeconds) {
		this.accessTokenValiditySeconds = accessTokenValiditySeconds;
	}

	public List<String> getPublicPaths() {
		return publicPaths;
	}

	public void setPublicPaths(List<String> paths) {
		this.publicPaths.clear();
		if (paths != null) {
			this.publicPaths.addAll(paths);
		}
	}
}
