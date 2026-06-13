package com.poemorder.app.web;

import com.poemorder.app.domain.review.ReviewStatus;
import com.poemorder.app.repo.OrderRepository;
import com.poemorder.app.repo.ReviewRepository;
import com.poemorder.app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PublicWebTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    ReviewRepository reviewRepository;

    @Test
    void publicPagesReturn200() throws Exception {
        for (String path : new String[]{"/", "/portfolio", "/pricing", "/reviews", "/contacts", "/order"}) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
    }

    @Test
    void order_validSubmissionRedirectsAndPersists() throws Exception {
        long before = orderRepository.count();

        mvc.perform(post("/order")
                        .with(csrf())
                        .header("X-Forwarded-For", "203.0.113.10")
                        .param("name", "Иван")
                        .param("phone", "+79990001122")
                        .param("description", "Хочу стих про кота"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/order"));

        assertThat(orderRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void order_honeypotIsSilentlyDropped() throws Exception {
        long before = orderRepository.count();

        mvc.perform(post("/order")
                        .with(csrf())
                        .header("X-Forwarded-For", "203.0.113.11")
                        .param("name", "Bot")
                        .param("phone", "1")
                        .param("description", "spam")
                        .param("website", "http://spam.example"))
                .andExpect(status().is3xxRedirection());

        assertThat(orderRepository.count()).isEqualTo(before); // ничего не сохранилось
    }

    @Test
    void order_withoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/order")
                        .header("X-Forwarded-For", "203.0.113.12")
                        .param("name", "Иван")
                        .param("phone", "1")
                        .param("description", "текст"))
                .andExpect(status().isForbidden());
    }

    @Test
    void order_validationErrorRedirectsBackWithoutPersisting() throws Exception {
        long before = orderRepository.count();

        mvc.perform(post("/order")
                        .with(csrf())
                        .header("X-Forwarded-For", "203.0.113.13")
                        .param("name", "")          // обязательное пусто
                        .param("phone", "")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/order"));

        assertThat(orderRepository.count()).isEqualTo(before);
    }

    @Test
    void review_validSubmissionCreatesPending() throws Exception {
        long pendingBefore = reviewRepository.findByStatusOrderByCreatedAtAsc(ReviewStatus.PENDING).size();

        mvc.perform(post("/reviews")
                        .with(csrf())
                        .header("X-Forwarded-For", "203.0.113.14")
                        .param("name", "Мария")
                        .param("text", "Чудесный стих, спасибо!")
                        .param("telegramUsername", "@maria")
                        .param("telegramPublic", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/reviews"));

        assertThat(reviewRepository.findByStatusOrderByCreatedAtAsc(ReviewStatus.PENDING).size())
                .isEqualTo(pendingBefore + 1);
    }

    @Test
    void rateLimit_blocksAfterConfiguredMax() throws Exception {
        String ip = "203.0.113.99";
        // тестовый лимит = 3 (application-test.properties)
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/order")
                            .with(csrf())
                            .header("X-Forwarded-For", ip)
                            .param("name", "N" + i).param("phone", "1").param("description", "d"))
                    .andExpect(status().is3xxRedirection());
        }
        // 4-й за окно → 429
        mvc.perform(post("/order")
                        .with(csrf())
                        .header("X-Forwarded-For", ip)
                        .param("name", "N4").param("phone", "1").param("description", "d"))
                .andExpect(status().isTooManyRequests());
    }
}
