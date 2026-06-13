package com.poemorder.app.integration.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * Best-effort отправка уведомлений в Telegram Bot API.
 * Пустой токен = уведомления выключены. Ошибки логируются,
 * но никогда не ломают запрос, в котором случилось уведомление.
 */
@Component
public class TelegramNotifier {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotifier.class);

    private final String chatId;
    private final boolean enabled;
    private final RestClient restClient;

    public TelegramNotifier(
            @Value("${app.telegram.bot-token:}") String botToken,
            @Value("${app.telegram.chat-id:}") String chatId
    ) {
        this.chatId = chatId;
        this.enabled = !botToken.isBlank() && !chatId.isBlank();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + botToken)
                .requestFactory(requestFactory)
                .build();

        if (!enabled) {
            log.info("Telegram notifications disabled (no TELEGRAM_BOT_TOKEN / TELEGRAM_CHAT_ID)");
        }
    }

    public void send(String text) {
        if (!enabled) return;

        try {
            restClient.post()
                    .uri("/sendMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("chat_id", chatId, "text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Telegram notification failed: {}", e.getMessage());
        }
    }
}
