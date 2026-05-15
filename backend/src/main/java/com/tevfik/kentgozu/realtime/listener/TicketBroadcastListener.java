package com.tevfik.kentgozu.realtime.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tevfik.kentgozu.ticket.TicketCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * ticket modülünden gelen {@link TicketCreatedEvent}'i dinler ve
 * JSON olarak {@code /topic/tickets} STOMP kanalına yayınlar.
 *
 * <p>Neden {@code @EventListener} (TransactionalEventListener değil)?
 * {@code @TransactionalEventListener(AFTER_COMMIT)}, Resilience4j {@code @RateLimiter}
 * proxy'si ile çakıştığında transaction senkronizasyon kaydı atlanabilir;
 * after-commit callback'inde fırlayan exception'lar da AbstractPlatformTransactionManager
 * tarafından sessizce yutulur. Broadcast, ticket zaten persist edildikten sonra
 * yayınlandığı için @EventListener yeterlidir.
 *
 * <p>Sessiz düşüş koruması: convertAndSend öncesi ObjectMapper ile manuel serialize
 * testi yapılır ve tüm adımlar loglanır. Hiçbir exception swallow edilmez.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketBroadcastListener {

	private static final String TOPIC_TICKETS = "/topic/tickets";

	private final SimpMessagingTemplate messagingTemplate;
	private final ObjectMapper objectMapper;

	@EventListener
	public void onTicketCreated(TicketCreatedEvent event) {
		log.info("[WS-BROADCAST] STEP-1 listener tetiklendi: ticketId={}", event.id());

		String json;
		try {
			json = objectMapper.writeValueAsString(event);
			log.info("[WS-BROADCAST] STEP-2 Jackson serialization OK: {}", json);
		} catch (JsonProcessingException ex) {
			log.error("[WS-BROADCAST] STEP-2 FAIL — Jackson serialize hatası ticketId={}", event.id(), ex);
			return;
		}

		try {
			messagingTemplate.convertAndSend(TOPIC_TICKETS, event);
			log.info("[WS-BROADCAST] STEP-3 broker dispatch OK: ticketId={}", event.id());
		} catch (Exception ex) {
			log.error("[WS-BROADCAST] STEP-3 FAIL — convertAndSend hatası ticketId={}", event.id(), ex);
		}
	}
}
