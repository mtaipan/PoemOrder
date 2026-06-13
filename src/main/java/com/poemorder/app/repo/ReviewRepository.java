package com.poemorder.app.repo;

import com.poemorder.app.domain.review.Review;
import com.poemorder.app.domain.review.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status);

    List<Review> findByStatusOrderByCreatedAtAsc(ReviewStatus status);

    // "top N" по статусу: и для пагинации в админке, и для главной (через .getContent())
    Page<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status, Pageable pageable);
}
