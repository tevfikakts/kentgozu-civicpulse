package com.tevfik.kentgozu.realtime.security;

import com.tevfik.kentgozu.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * STOMP CONNECT frame'indeki {@code Authorization: Bearer <token>} başlığını doğrular.
 * Standart WebSocket API HTTP başlığı taşıyamadığından token URL'de DEĞİL, bu frame'de iletilir.
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor =
				MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
			return message;
		}

		String authHeader = accessor.getFirstNativeHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
			return message;
		}

		String token = authHeader.substring(BEARER_PREFIX.length()).trim();
		Optional<Map<String, Object>> claims = jwtService.parseAndValidate(token);
		claims.ifPresent(c -> {
			String subject = Optional.ofNullable(c.get("sub"))
					.map(Object::toString)
					.orElse("")
					.trim();
			if (!subject.isEmpty()) {
				Collection<GrantedAuthority> authorities = jwtService.extractAuthorities(c);
				User principal = new User(subject, "", authorities);
				UsernamePasswordAuthenticationToken auth =
						new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
				accessor.setUser(auth);
			}
		});

		return message;
	}
}
