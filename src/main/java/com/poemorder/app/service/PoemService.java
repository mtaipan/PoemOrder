package com.poemorder.app.service;

import com.poemorder.app.domain.poem.Poem;
import com.poemorder.app.domain.poem.PoemStatus;
import com.poemorder.app.repo.PoemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PoemService {

    private static final Logger log = LoggerFactory.getLogger(PoemService.class);

    private final PoemRepository poemRepository;

    public PoemService(PoemRepository poemRepository) {
        this.poemRepository = poemRepository;
    }

    @Transactional(readOnly = true)
    public Page<Poem> list(Pageable pageable) {
        return poemRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Poem getOrThrow(Long id) {
        return poemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Poem not found: " + id));
    }

    @Transactional
    public Poem create(Poem poem) {
        // защита от подмены полей через POST: id и createdAt задаёт только система
        poem.setId(null);
        poem.setCreatedAt(null);
        Poem saved = poemRepository.save(poem);
        log.info("Poem #{} created (title='{}', status={})", saved.getId(), saved.getTitle(), saved.getStatus());
        return saved;
    }

    @Transactional
    public Poem update(Long id, Poem updated) {
        Poem current = getOrThrow(id);

        current.setTitle(updated.getTitle());
        current.setExcerpt(updated.getExcerpt());
        current.setBody(updated.getBody());
        current.setStatus(updated.getStatus());

        Poem saved = poemRepository.save(current);
        log.info("Poem #{} updated (status={})", id, saved.getStatus());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        poemRepository.deleteById(id);
        log.info("Poem #{} deleted", id);
    }

    @Transactional(readOnly = true)
    public List<Poem> publishedAll() {
        return poemRepository.findAllByStatusOrderByUpdatedAtDesc(PoemStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public List<Poem> publishedForHomepage(int limit) {
        return poemRepository.findAllByStatusOrderByUpdatedAtDesc(PoemStatus.PUBLISHED, PageRequest.of(0, limit));
    }
}
