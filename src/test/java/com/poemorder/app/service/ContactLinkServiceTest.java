package com.poemorder.app.service;

import com.poemorder.app.domain.settings.ContactLink;
import com.poemorder.app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ContactLinkServiceTest extends AbstractIntegrationTest {

    @Autowired
    ContactLinkService service;

    @Test
    void upsert_acceptsAllowedSchemes() {
        for (String href : List.of("https://t.me/poet", "http://site.ru", "mailto:a@b.ru", "tel:+79990001122")) {
            ContactLink saved = service.upsert(null, "L", "v", href, 0, true);
            assertThat(saved.getHref()).isEqualTo(href);
        }
    }

    @Test
    void upsert_rejectsJavascriptScheme() {
        assertThatThrownBy(() -> service.upsert(null, "XSS", "click", "javascript:alert(1)", 0, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void upsert_blankHrefStoredAsNull() {
        assertThat(service.upsert(null, "Просто текст", "v", "   ", 0, true).getHref()).isNull();
        assertThat(service.upsert(null, "Без ссылки", "v", null, 0, true).getHref()).isNull();
    }

    @Test
    void publicList_returnsOnlyEnabledSortedBySortOrder() {
        service.upsert(null, "Second", "v", null, 2, true);
        service.upsert(null, "First", "v", null, 1, true);
        service.upsert(null, "Disabled", "v", null, 0, false);

        List<ContactLink> list = service.publicList();

        assertThat(list).extracting(ContactLink::getLabel).doesNotContain("Disabled");
        // отсортировано по sortOrder возрастающе
        assertThat(list).isSortedAccordingTo((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
        assertThat(list).extracting(ContactLink::getLabel).containsSubsequence("First", "Second");
    }
}
