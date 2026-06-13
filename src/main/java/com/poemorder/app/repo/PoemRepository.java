package com.poemorder.app.repo;

import com.poemorder.app.domain.poem.Poem;
import com.poemorder.app.domain.poem.PoemStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PoemRepository extends JpaRepository<Poem, Long> {

    List<Poem> findAllByOrderByUpdatedAtDesc();

    List<Poem> findAllByStatusOrderByUpdatedAtDesc(PoemStatus status);

    List<Poem> findAllByStatusOrderByUpdatedAtDesc(PoemStatus status, Pageable pageable);
}
