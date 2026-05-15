package com.tevfik.kentgozu.security.config;

import com.tevfik.kentgozu.security.jwt.JwtService;
import com.tevfik.kentgozu.security.web.JwtAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtAuthenticationFilter jwtAuthenticationFilter,
			JwtSecurityProperties jwtSecurityProperties)
			throws Exception {
		String[] publicPatterns = jwtSecurityProperties.getPublicPaths().toArray(String[]::new);
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.anonymous(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(publicPatterns).permitAll()
						.requestMatchers("/api/auth/**").permitAll()
						.requestMatchers("/api/tickets/analyze").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/tickets").permitAll()
						.requestMatchers("/ws/**").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	JwtAuthenticationFilter jwtAuthenticationFilter(
			JwtService jwtService,
			JwtSecurityProperties jwtSecurityProperties,
			HandlerExceptionResolver handlerExceptionResolver) {
		return new JwtAuthenticationFilter(jwtService, jwtSecurityProperties, handlerExceptionResolver);
	}
}
