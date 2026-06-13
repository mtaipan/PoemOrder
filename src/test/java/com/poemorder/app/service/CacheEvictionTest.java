package com.poemorder.app.service;

import com.poemorder.app.domain.settings.ContactLink;
import com.poemorder.app.domain.settings.SiteSettings;
import com.poemorder.app.repo.ContactLinkRepository;
import com.poemorder.app.repo.SiteSettingsRepository;
import com.poemorder.app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, что справочные данные реально кэшируются и сбрасываются
 * при изменении через сервис. Тест не транзакционный (кэш живёт вне транзакции),
 * поэтому сам убирает за собой.
 */
class CacheEvictionTest extends AbstractIntegrationTest {

    @Autowired
    ContactLinkService contactLinkService;
    @Autowired
    ContactLinkRepository contactLinkRepository;
    @Autowired
    SiteSettingsService siteSettingsService;
    @Autowired
    SiteSettingsRepository siteSettingsRepository;
    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    @AfterEach
    void resetState() {
        contactLinkRepository.deleteAll();
        cacheManager.getCache("contactLinks").clear();
        cacheManager.getCache("siteSettings").clear();
    }

    private ContactLink enabledLink(String label) {
        ContactLink c = new ContactLink();
        c.setLabel(label);
        c.setValue("v");
        c.setSortOrder(0);
        c.setEnabled(true);
        return c;
    }

    @Test
    void publicList_isCachedUntilUpsertEvicts() {
        contactLinkRepository.save(enabledLink("A"));

        // первый вызов кэширует
        assertThat(contactLinkService.publicList()).extracting(ContactLink::getLabel).containsExactly("A");

        // прямая запись в обход сервиса не сбрасывает кэш
        contactLinkRepository.save(enabledLink("B"));
        assertThat(contactLinkService.publicList()).extracting(ContactLink::getLabel)
                .containsExactly("A"); // всё ещё из кэша

        // изменение через сервис сбрасывает кэш (@CacheEvict)
        contactLinkService.upsert(null, "C", "v", null, 0, true);
        assertThat(contactLinkService.publicList()).extracting(ContactLink::getLabel)
                .contains("A", "B", "C");
    }

    @Test
    void siteSettings_isCachedUntilUpdateEvicts() {
        SiteSettings original = siteSettingsService.get(); // кэширует
        String originalTitle = original.getHeroTitle();

        // прямая запись в обход сервиса
        SiteSettings row = siteSettingsRepository.findById((short) 1).orElseThrow();
        row.setHeroTitle("DIRECT");
        siteSettingsRepository.saveAndFlush(row);

        assertThat(siteSettingsService.get().getHeroTitle())
                .isEqualTo(originalTitle); // всё ещё из кэша

        // изменение через сервис сбрасывает кэш
        SiteSettings changes = new SiteSettings();
        changes.setHeroTitle("VIA_SERVICE");
        changes.setHeroSubtitle(original.getHeroSubtitle());
        siteSettingsService.update(changes);

        assertThat(siteSettingsService.get().getHeroTitle()).isEqualTo("VIA_SERVICE");

        // вернуть исходный заголовок, чтобы не влиять на другие тесты
        SiteSettings restore = new SiteSettings();
        restore.setHeroTitle(originalTitle);
        restore.setHeroSubtitle(original.getHeroSubtitle());
        siteSettingsService.update(restore);
    }
}
