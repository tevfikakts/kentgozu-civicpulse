package com.tevfik.kentgozu.security.jwt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tevfik.kentgozu.security.config.JwtSecurityProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWT üretimi, HMAC-SHA256 imzalama, doğrulama ve taleplerden kullanıcı/rol okuma.
 * Harici JWT kütüphanesi kullanılmaz; imza {@link Mac} ile üretilir.
 */
@Service
public class JwtService {

	private static final String ALG = "HS256";
	private static final String TYP = "JWT";
	private static final String ROLES_CLAIM = "roles";
	private static final String ISS = "iss";
	private static final String SUB = "sub";
	private static final String IAT = "iat";
	private static final String EXP = "exp";

	private final JwtSecurityProperties properties;
	private final ObjectMapper objectMapper;
	private byte[] secretBytes;

	public JwtService(JwtSecurityProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@PostConstruct
	void initSigningKey() {
		String secret = properties.getSecret();
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException(
					"kentgozu.security.jwt.secret must be set (e.g. JWT_SECRET environment variable); never commit production secrets.");
		}
		this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < 32) {
			throw new IllegalStateException(
					"kentgozu.security.jwt.secret must be at least 256 bits (32 bytes) for HS256.");
		}
	}

	/**
	 * Yeni erişim jetonu üretir (imza: HMAC-SHA256).
	 */
	public String createAccessToken(String subject, Collection<PlatformRole> roles) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(properties.getAccessTokenValiditySeconds());
		List<String> roleNames = roles.stream().map(Enum::name).toList();
		try {
			Map<String, Object> header = Map.of("alg", ALG, "typ", TYP);
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put(ISS, properties.getIssuer());
			payload.put(SUB, subject);
			payload.put(ROLES_CLAIM, roleNames);
			payload.put(IAT, now.getEpochSecond());
			payload.put(EXP, exp.getEpochSecond());

			String headerB64 = base64UrlJson(header);
			String payloadB64 = base64UrlJson(payload);
			String signingInput = headerB64 + "." + payloadB64;
			String signatureB64 = sign(signingInput);
			return signingInput + "." + signatureB64;
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to encode JWT", e);
		}
	}

	/**
	 * İmzayı ve süre dolumunu doğrular; geçerliyse talep haritasını döndürür.
	 */
	public Optional<Map<String, Object>> parseAndValidate(String token) {
		try {
			return Optional.of(parseValidClaimsOrThrow(token));
		}
		catch (ExpiredJwtException | InvalidJwtException e) {
			return Optional.empty();
		}
	}

	/**
	 * Geçerli talepleri döndürür; süre dolumunda {@link ExpiredJwtException}, aksi doğrulama hatalarında
	 * {@link InvalidJwtException} fırlatır (filtre + {@code HandlerExceptionResolver} senaryosu için).
	 */
	public Map<String, Object> parseValidClaimsOrThrow(String token) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length != 3) {
				throw new InvalidJwtException("Malformed JWT");
			}
			String signingInput = parts[0] + "." + parts[1];
			String expectedSig = sign(signingInput);
			if (!constantTimeEqualsSignature(expectedSig, parts[2])) {
				throw new InvalidJwtException("Invalid JWT signature");
			}
			String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
			Map<String, Object> claims = objectMapper.readValue(payloadJson, new TypeReference<>() {
			});
			if (!Objects.equals(properties.getIssuer(), claims.get(ISS))) {
				throw new InvalidJwtException("Invalid JWT issuer");
			}
			long exp = toLong(claims.get(EXP));
			if (exp <= 0) {
				throw new InvalidJwtException("Invalid JWT expiration claim");
			}
			if (Instant.now().getEpochSecond() >= exp) {
				throw new ExpiredJwtException("JWT expired");
			}
			return claims;
		}
		catch (ExpiredJwtException | InvalidJwtException e) {
			throw e;
		}
		catch (Exception e) {
			throw new InvalidJwtException("JWT validation failed", e);
		}
	}

	public Optional<String> extractSubject(String token) {
		return parseAndValidate(token).map(c -> String.valueOf(c.get(SUB)));
	}

	public Set<GrantedAuthority> extractAuthorities(Map<String, Object> claims) {
		Object raw = claims.get(ROLES_CLAIM);
		if (raw == null) {
			return Set.of();
		}
		if (raw instanceof Collection<?> collection) {
			LinkedHashSet<GrantedAuthority> authorities = new LinkedHashSet<>();
			for (Object item : collection) {
				if (item == null) {
					continue;
				}
				String name = item.toString().trim();
				if (name.isEmpty()) {
					continue;
				}
				authorities.addAll(toAuthorities(name));
			}
			return authorities;
		}
		return Set.of();
	}

	public Set<GrantedAuthority> extractAuthorities(String token) {
		return parseAndValidate(token).map(this::extractAuthorities).orElseGet(Set::of);
	}

	public Set<String> extractRoleNames(Map<String, Object> claims) {
		return extractAuthorities(claims).stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private String base64UrlJson(Map<String, Object> map) throws Exception {
		String json = objectMapper.writeValueAsString(map);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}

	private String sign(String signingInput) throws NoSuchAlgorithmException, InvalidKeyException {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
		byte[] sig = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
		return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v));
		}
		catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean constantTimeEqualsSignature(String expectedB64, String actualB64) {
		try {
			byte[] x = Base64.getUrlDecoder().decode(expectedB64);
			byte[] y = Base64.getUrlDecoder().decode(actualB64);
			if (x.length != y.length) {
				return false;
			}
			int r = 0;
			for (int i = 0; i < x.length; i++) {
				r |= x[i] ^ y[i];
			}
			return r == 0;
		}
		catch (IllegalArgumentException e) {
			return false;
		}
	}

	private static Collection<GrantedAuthority> toAuthorities(String roleName) {
		try {
			PlatformRole role = PlatformRole.valueOf(roleName);
			return List.of(new SimpleGrantedAuthority(role.authority()));
		}
		catch (IllegalArgumentException ex) {
			if (roleName.startsWith("ROLE_")) {
				return List.of(new SimpleGrantedAuthority(roleName));
			}
			return List.of(new SimpleGrantedAuthority("ROLE_" + roleName));
		}
	}
}
