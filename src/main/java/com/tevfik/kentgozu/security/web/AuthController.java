package com.tevfik.kentgozu.security.web;

import com.tevfik.kentgozu.security.api.LoginRequest;
import com.tevfik.kentgozu.security.api.LoginResponse;
import com.tevfik.kentgozu.security.config.JwtSecurityProperties;
import com.tevfik.kentgozu.security.jwt.JwtService;
import com.tevfik.kentgozu.security.jwt.PlatformRole;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@RestController
public class AuthController {

	private final JwtService jwtService;
	private final JwtSecurityProperties jwtSecurityProperties;

	public AuthController(JwtService jwtService, JwtSecurityProperties jwtSecurityProperties) {
		this.jwtService = jwtService;
		this.jwtSecurityProperties = jwtSecurityProperties;
	}

	@PostMapping("/api/auth/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		PlatformRole role = parseRole(request.role());
		String token = jwtService.createAccessToken(request.email().trim().toLowerCase(Locale.ROOT), List.of(role));
		LoginResponse body = new LoginResponse(token, "Bearer", jwtSecurityProperties.getAccessTokenValiditySeconds());
		return ResponseEntity.status(HttpStatus.OK).body(body);
	}

	private static PlatformRole parseRole(String raw) {
		try {
			return PlatformRole.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role must be CITIZEN or ADMIN");
		}
	}
}
