package com.poemorder.app.service;

import com.poemorder.app.domain.poem.Poem;
import com.poemorder.app.domain.poem.PoemStatus;
import com.poemorder.app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PoemServiceTest extends AbstractIntegrationTest {

    @Autowired
    PoemService poemService;

    private Poem poem(String title, PoemStatus status) {
        Poem p = new Poem();
        p.setTitle(title);
        p.setBody("тело стиха");
        p.setStatus(status);
        return p;
    }

    @Test
    void create_ignoresClientSuppliedIdAndCreatedAt() {
        Poem p = poem("Подделка", PoemStatus.DRAFT);
        p.setId(999_999L);                          // попытка задать чужой id
        p.setCreatedAt(Instant.EPOCH);              // попытка задать дату

        Poem saved = poemService.create(p);

        assertThat(saved.getId()).isNotNull().isNotEqualTo(999_999L);
        assertThat(saved.getCreatedAt()).isNotNull().isAfter(Instant.EPOCH);
    }

    @Test
    void publishedAll_returnsOnlyPublished() {
        poemService.create(poem("Черновик", PoemStatus.DRAFT));
        poemService.create(poem("Опубликован", PoemStatus.PUBLISHED));

        assertThat(poemService.publishedAll()).extracting(Poem::getStatus)
                .containsOnly(PoemStatus.PUBLISHED);
        assertThat(poemService.publishedAll()).extracting(Poem::getTitle)
                .contains("Опубликован")
                .doesNotContain("Черновик");
    }

    @Test
    void publishedForHomepage_respectsLimit() {
        for (int i = 0; i < 4; i++) {
            poemService.create(poem("Pub-" + i, PoemStatus.PUBLISHED));
        }

        assertThat(poemService.publishedForHomepage(2)).hasSize(2);
    }

    @Test
    void update_changesEditableFields() {
        Poem saved = poemService.create(poem("Старый", PoemStatus.DRAFT));

        Poem changes = poem("Новый", PoemStatus.PUBLISHED);
        changes.setExcerpt("новый отрывок");
        poemService.update(saved.getId(), changes);

        Poem reloaded = poemService.getOrThrow(saved.getId());
        assertThat(reloaded.getTitle()).isEqualTo("Новый");
        assertThat(reloaded.getStatus()).isEqualTo(PoemStatus.PUBLISHED);
        assertThat(reloaded.getExcerpt()).isEqualTo("новый отрывок");
    }
}
