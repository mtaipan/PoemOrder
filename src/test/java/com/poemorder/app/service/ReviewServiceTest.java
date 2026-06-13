package com.poemorder.app.service;

import com.poemorder.app.domain.review.Review;
import com.poemorder.app.domain.review.ReviewStatus;
import com.poemorder.app.dto.ReviewForm;
import com.poemorder.app.repo.ReviewRepository;
import com.poemorder.app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ReviewServiceTest extends AbstractIntegrationTest {

    @Autowired
    ReviewService reviewService;
    @Autowired
    ReviewRepository reviewRepository;

    private ReviewForm form(String name, String text) {
        ReviewForm f = new ReviewForm();
        f.setName(name);
        f.setText(text);
        return f;
    }

    @Test
    void createPending_savesAsPendingAndStripsAtFromUsername() {
        ReviewForm f = form("  Мария  ", "  Отличный стих  ");
        f.setTelegramUsername("@maria");
        f.setTelegramPublic(true);

        reviewService.createPending(f);

        List<Review> pending = reviewService.getPending();
        assertThat(pending).extracting(Review::getName).contains("Мария");      // trim
        Review r = pending.stream().filter(x -> "Мария".equals(x.getName())).findFirst().orElseThrow();
        assertThat(r.getStatus()).isEqualTo(ReviewStatus.PENDING);
        assertThat(r.getText()).isEqualTo("Отличный стих");                     // trim
        assertThat(r.getTelegramUsername()).isEqualTo("maria");                 // @ срезан
        assertThat(r.isTelegramPublic()).isTrue();
        assertThat(r.getCreatedAt()).isNotNull();
    }

    @Test
    void createPending_blankUsernameBecomesNull() {
        ReviewForm f = form("Аноним", "текст");
        f.setTelegramUsername("@");

        reviewService.createPending(f);

        Review r = reviewService.getPending().stream()
                .filter(x -> "Аноним".equals(x.getName())).findFirst().orElseThrow();
        assertThat(r.getTelegramUsername()).isNull();
    }

    @Test
    void approve_makesReviewVisibleInApproved() {
        reviewService.createPending(form("Одобряемый", "текст"));
        Review pending = reviewService.getPending().stream()
                .filter(x -> "Одобряемый".equals(x.getName())).findFirst().orElseThrow();

        reviewService.approve(pending.getId());

        assertThat(reviewService.getApproved()).extracting(Review::getName).contains("Одобряемый");
        assertThat(reviewService.getPending()).extracting(Review::getName).doesNotContain("Одобряемый");
    }

    @Test
    void hideContact_clearsTelegram() {
        ReviewForm f = form("Контактный", "текст");
        f.setTelegramUsername("nick");
        f.setTelegramPublic(true);
        reviewService.createPending(f);
        Review r = reviewService.getPending().stream()
                .filter(x -> "Контактный".equals(x.getName())).findFirst().orElseThrow();

        reviewService.hideContact(r.getId());

        Review updated = reviewRepository.findById(r.getId()).orElseThrow();
        assertThat(updated.getTelegramUsername()).isNull();
        assertThat(updated.isTelegramPublic()).isFalse();
    }

    @Test
    void findApprovedForHomepage_respectsLimit() {
        for (int i = 0; i < 5; i++) {
            reviewService.createPending(form("Hp-" + i, "t"));
        }
        reviewService.getPending().forEach(r -> reviewService.approve(r.getId()));

        assertThat(reviewService.findApprovedForHomepage(3)).hasSizeLessThanOrEqualTo(3);
        assertThat(reviewService.getApproved().size()).isGreaterThanOrEqualTo(5);
    }
}
