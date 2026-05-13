package com.tevfik.kentgozu.ticket.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tevfik.kentgozu.security.jwt.JwtService;
import com.tevfik.kentgozu.security.jwt.PlatformRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
		properties = {
				"spring.autoconfigure.exclude="
						+ "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
						+ "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration,"
						+ "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
				"spring.jpa.hibernate.ddl-auto=update"
		})
@AutoConfigureMockMvc
@Transactional
class TicketControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JwtService jwtService;

	private String bearerToken() {
		return "Bearer " + jwtService.createAccessToken("admin@kentgozu.com", List.of(PlatformRole.ADMIN));
	}

	private Map<String, Object> validTicketBody() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("reporterEmail", "admin@kentgozu.com");
		m.put("title", "Yol çukuru");
		m.put("description", "");
		m.put("status", "submitted");
		m.put("longitude", 28.9784);
		m.put("latitude", 41.0082);
		return m;
	}

	@Test
	@DisplayName("1) Geçerli payload ile POST /api/tickets 201 ve gövde doğrulanır")
	void createTicket_withValidPayload_returnsCreated() throws Exception {
		mockMvc.perform(post("/api/tickets")
						.header(HttpHeaders.AUTHORIZATION, bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validTicketBody())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title", is("Yol çukuru")))
				.andExpect(jsonPath("$.status", is("SUBMITTED")))
				.andExpect(jsonPath("$.reporterEmail", is("admin@kentgozu.com")))
				.andExpect(jsonPath("$.longitude").exists())
				.andExpect(jsonPath("$.latitude").exists());
	}

	@Test
	@DisplayName("2) Geçersiz status ile 400 ve açıklayıcı mesaj")
	void createTicket_withInvalidStatus_returns400() throws Exception {
		Map<String, Object> body = validTicketBody();
		body.put("status", "OPEN");
		mockMvc.perform(post("/api/tickets")
						.header(HttpHeaders.AUTHORIZATION, bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorType", is("response_status")))
				.andExpect(jsonPath("$.detail", containsString("Invalid ticket status")))
				.andExpect(jsonPath("$.detail", containsString("SUBMITTED")));
	}

	@Test
	@DisplayName("3) Boş title ile validation 400")
	void createTicket_withBlankTitle_returnsValidation400() throws Exception {
		Map<String, Object> body = validTicketBody();
		body.put("title", "   ");
		mockMvc.perform(post("/api/tickets")
						.header(HttpHeaders.AUTHORIZATION, bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorType", is("validation")))
				.andExpect(jsonPath("$.violations", hasSize(1)))
				.andExpect(jsonPath("$.violations[0].field", is("title")));
	}

	@Test
	@DisplayName("4) Geçersiz e-posta ile validation 400")
	void createTicket_withInvalidEmail_returnsValidation400() throws Exception {
		Map<String, Object> body = validTicketBody();
		body.put("reporterEmail", "not-an-email");
		mockMvc.perform(post("/api/tickets")
						.header(HttpHeaders.AUTHORIZATION, bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorType", is("validation")))
				.andExpect(jsonPath("$.violations[0].field", is("reporterEmail")));
	}

	@Test
	@DisplayName("5) latitude eksik (null) ile validation 400")
	void createTicket_withMissingLatitude_returnsValidation400() throws Exception {
		String json = """
				{"reporterEmail":"admin@kentgozu.com","title":"t","description":"d","status":"SUBMITTED","longitude":29.0}
				""";
		mockMvc.perform(post("/api/tickets")
						.header(HttpHeaders.AUTHORIZATION, bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorType", is("validation")))
				.andExpect(jsonPath("$.violations[0].field", is("latitude")));
	}

	@Test
	@DisplayName("6) Bozuk JSON ile deserialization 400 (json_parse)")
	void createTicket_withMalformedJson_returns400() throws Exception {
		mockMvc.perform(post("/api/tickets")
						.header(HttpHeaders.AUTHORIZATION, bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ not valid json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorType", is("json_parse")))
				.andExpect(jsonPath("$.title", is("Malformed JSON")));
	}

	@Test
	@DisplayName("7) POST /api/tickets endpoint entegrasyonu (auth + JSON + 201)")
	void postTicketsEndpoint_integration() throws Exception {
		mockMvc.perform(post("/api/tickets")
						.header(HttpHeaders.AUTHORIZATION, bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validTicketBody())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists());
	}
}
