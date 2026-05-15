package com.tevfik.kentgozu;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;

@SpringBootTest(
		properties = {
				"spring.autoconfigure.exclude="
						+ "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
						+ "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration,"
						+ "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
				"spring.jpa.hibernate.ddl-auto=update"
		})
class KentgozuApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void verifyModulithArchitecture() {
        // Modüler mimari sınırlarını ve sızıntıları test eden katı yargıç
        ApplicationModules.of(KentgozuApplication.class).verify();
    }
}