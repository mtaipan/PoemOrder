package com.poemorder.app.integration.telegram;

import com.poemorder.app.domain.order.Order;
import com.poemorder.app.domain.review.Review;
import org.springframework.stereotype.Component;

@Component
public class TelegramMessageFormatter {

    private static final int MAX_TEXT_PREVIEW = 500;

    public String newOrder(Order order) {
        StringBuilder sb = new StringBuilder("🆕 Новая заявка #").append(order.getId()).append('\n');
        sb.append("Имя: ").append(order.getName()).append('\n');
        sb.append("Телефон: ").append(order.getPhone()).append('\n');
        if (order.getSocial() != null && !order.getSocial().isBlank()) {
            sb.append("Соцсеть: ").append(order.getSocial()).append('\n');
        }
        sb.append('\n').append(preview(order.getDescription()));
        return sb.toString();
    }

    public String newReview(Review review) {
        StringBuilder sb = new StringBuilder("💬 Новый отзыв на модерацию #").append(review.getId()).append('\n');
        sb.append("Имя: ").append(review.getName()).append('\n');
        if (review.getTelegramUsername() != null) {
            sb.append("Telegram: @").append(review.getTelegramUsername()).append('\n');
        }
        sb.append('\n').append(preview(review.getText()));
        return sb.toString();
    }

    private String preview(String text) {
        if (text == null) return "";
        return text.length() <= MAX_TEXT_PREVIEW ? text : text.substring(0, MAX_TEXT_PREVIEW) + "…";
    }
}
