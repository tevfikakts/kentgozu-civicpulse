package com.tevfik.kentgozu.user.bootstrap;

import com.tevfik.kentgozu.user.domain.User;
import com.tevfik.kentgozu.user.persistence.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DatabaseSeeder implements CommandLineRunner {

	private static final String ADMIN_EMAIL = "admin@kentgozu.com";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public DatabaseSeeder(UserRepository userRepository) {
		this.userRepository = userRepository;
		this.passwordEncoder = new BCryptPasswordEncoder();
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (userRepository.findByEmailIgnoreCase(ADMIN_EMAIL).isPresent()) {
			return;
		}
		User admin = new User();
		admin.setEmail(ADMIN_EMAIL);
		admin.setDisplayName("Administrator");
		admin.setPasswordHash(passwordEncoder.encode("password"));
		userRepository.saveAndFlush(admin);
	}
}
