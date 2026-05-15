package com.tevfik.kentgozu.security.web;

import com.tevfik.kentgozu.security.config.JwtSecurityProperties;
import com.tevfik.kentgozu.security.jwt.ExpiredJwtException;
import com.tevfik.kentgozu.security.jwt.InvalidJwtException;
import com.tevfik.kentgozu.security.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Bearer JWT doğrular; süre dolumu {@link ExpiredJwtException} için
 * {@link HandlerExceptionResolver} ile {@code @RestControllerAdvice} yolunu kullanır.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final JwtSecurityProperties securityProperties;
	private final HandlerExceptionResolver handlerExceptionResolver;
	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	public JwtAuthenticationFilter(
			JwtService jwtService,
			JwtSecurityProperties securityProperties,
			HandlerExceptionResolver handlerExceptionResolver) {
		this.jwtService = jwtService;
		this.securityProperties = securityProperties;
		this.handlerExceptionResolver = handlerExceptionResolver;
	}

	@Override
	protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
		String path = request.getServletPath();
		for (String pattern : securityProperties.getPublicPaths()) {
			if (pathMatcher.match(pattern, path)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = header.substring(BEARER_PREFIX.length()).trim();
		final Map<String, Object> claims;
		try {
			claims = jwtService.parseValidClaimsOrThrow(token);
		}
		catch (ExpiredJwtException | InvalidJwtException ex) {
			SecurityContextHolder.clearContext();
			delegateJwtException(request, response, ex);
			return;
		}

		String subject = Optional.ofNullable(claims.get("sub")).map(Object::toString).orElse("").trim();
		if (subject.isEmpty()) {
			SecurityContextHolder.clearContext();
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		Collection<GrantedAuthority> authorities = jwtService.extractAuthorities(claims);
		User principal = new User(subject, "", authorities);
		UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(authentication);

		filterChain.doFilter(request, response);
	}

	private void delegateJwtException(HttpServletRequest request, HttpServletResponse response, Exception ex)
			throws IOException {
		if (response.isCommitted()) {
			return;
		}
		try {
			if (handlerExceptionResolver.resolveException(request, response, null, ex) == null && !response.isCommitted()) {
				writeMinimalUnauthorized(response, ex.getMessage());
			}
		}
		catch (Exception ignored) {
			if (!response.isCommitted()) {
				writeMinimalUnauthorized(response, ex.getMessage());
			}
		}
	}

	private static void writeMinimalUnauthorized(HttpServletResponse response, String detail) throws IOException {
		response.resetBuffer();
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		String safe = detail == null ? "" : detail.replace("\"", "\\\"");
		response.getWriter().write("{\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"" + safe + "\"}");
		response.flushBuffer();
	}
}
