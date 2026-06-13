package com.poemorder.app.integration.telegram;

import com.poemorder.app.domain.order.Order;
import com.poemorder.app.domain.review.Review;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Чистый юнит-тест форматирования — без Spring и БД.
 */
class TelegramMessageFormatterTest {

    private final TelegramMessageFormatter formatter = new TelegramMessageFormatter();

    @Test
    void newOrder_includesAllFilledFields() {
        Order order = new Order();
        order.setName("Иван");
        order.setPhone("+79990001122");
        order.setSocial("@ivan");
        order.setDescription("Хочу стих про кота");

        String msg = formatter.newOrder(order);

        assertThat(msg)
                .contains("Новая заявка")
                .contains("Имя: Иван")
                .contains("Телефон: +79990001122")
                .contains("Соцсеть: @ivan")
                .contains("Хочу стих про кота");
    }

    @Test
    void newOrder_omitsSocialWhenBlank() {
        Order order = new Order();
        order.setName("Без соцсети");
        order.setPhone("123");
        order.setSocial("   ");
        order.setDescription("текст");

        assertThat(formatter.newOrder(order)).doesNotContain("Соцсеть:");
    }

    @Test
    void newOrder_truncatesLongDescription() {
        Order order = new Order();
        order.setName("X");
        order.setPhone("1");
        order.setDescription("a".repeat(600));

        String msg = formatter.newOrder(order);

        assertThat(msg).contains("…");
        // 500 символов превью + многоточие, исходные 600 целиком не попадают
        assertThat(msg).doesNotContain("a".repeat(600));
        assertThat(msg).contains("a".repeat(500));
    }

    @Test
    void newReview_includesTelegramWhenPresent() {
        Review review = new Review();
        review.setName("Мария");
        review.setText("Чудесный стих, спасибо!");
        review.setTelegramUsername("maria");

        String msg = formatter.newReview(review);

        assertThat(msg)
                .contains("Новый отзыв")
                .contains("Имя: Мария")
                .contains("Telegram: @maria")
                .contains("Чудесный стих, спасибо!");
    }

    @Test
    void newReview_omitsTelegramWhenNull() {
        Review review = new Review();
        review.setName("Аноним");
        review.setText("ок");
        review.setTelegramUsername(null);

        assertThat(formatter.newReview(review)).doesNotContain("Telegram:");
    }
}
