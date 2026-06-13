package com.poemorder.app.service;

import com.poemorder.app.domain.review.Review;
import com.poemorder.app.domain.review.ReviewStatus;
import com.poemorder.app.dto.ReviewForm;
import com.poemorder.app.integration.telegram.TelegramMessageFormatter;
import com.poemorder.app.integration.telegram.TelegramNotifier;
import com.poemorder.app.repo.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository repo;
    private final TelegramNotifier telegramNotifier;
    private final TelegramMessageFormatter telegramFormatter;

    public ReviewService(ReviewRepository repo,
                         TelegramNotifier telegramNotifier,
                         TelegramMessageFormatter telegramFormatter) {
        this.repo = repo;
        this.telegramNotifier = telegramNotifier;
        this.telegramFormatter = telegramFormatter;
    }

    @Transactional
    public void createPending(ReviewForm form) {
        Review r = new Review();
        r.setName(form.getName().trim());
        r.setText(form.getText().trim());

        String tg = form.getTelegramUsername();
        if (tg != null) {
            tg = tg.trim();
            if (tg.startsWith("@")) tg = tg.substring(1);
            if (tg.isBlank()) tg = null;
        }

        r.setTelegramUsername(tg);
        r.setTelegramPublic(form.isTelegramPublic());

        r.setStatus(ReviewStatus.PENDING);
        // createdAt проставляется в @PrePersist (как у Poem/Order)

        Review saved = repo.save(r);
        log.info("New review #{} submitted for moderation (name='{}')", saved.getId(), saved.getName());
        telegramNotifier.send(telegramFormatter.newReview(saved));
    }

    @Transactional(readOnly = true)
    public List<Review> getApproved() {
        return repo.findByStatusOrderByCreatedAtDesc(ReviewStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    public Page<Review> getApproved(Pageable pageable) {
        return repo.findByStatusOrderByCreatedAtDesc(ReviewStatus.APPROVED, pageable);
    }

    @Transactional(readOnly = true)
    public List<Review> getPending() {
        return repo.findByStatusOrderByCreatedAtAsc(ReviewStatus.PENDING);
    }

    @Transactional
    public void approve(Long id) {
        Review r = repo.findById(id).orElseThrow();
        r.setStatus(ReviewStatus.APPROVED);
        repo.save(r);
        log.info("Review #{} approved", id);
    }

    @Transactional
    public void hideContact(Long id) {
        Review r = repo.findById(id).orElseThrow();
        r.setTelegramUsername(null);
        r.setTelegramPublic(false);
        repo.save(r);
        log.info("Review #{} contact hidden", id);
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
        log.info("Review #{} deleted", id);
    }

    @Transactional(readOnly = true)
    public List<Review> findApprovedForHomepage(int limit) {
        return repo.findByStatusOrderByCreatedAtDesc(ReviewStatus.APPROVED, PageRequest.of(0, limit))
                .getContent();
    }
}
