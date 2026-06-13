package com.poemorder.app.service;

import com.poemorder.app.domain.settings.SiteSettings;
import com.poemorder.app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class SiteSettingsServiceTest extends AbstractIntegrationTest {

    @Autowired
    SiteSettingsService service;

    @Test
    void get_returnsSeededSingleton() {
        SiteSettings s = service.get();
        assertThat(s.getId()).isEqualTo((short) 1);
        assertThat(s.getHeroTitle()).isNotBlank();
        assertThat(s.getHeroSubtitle()).isNotBlank();
    }

    @Test
    void update_changesEditableFields() {
        SiteSettings changes = new SiteSettings();
        changes.setHeroTitle("Обновлённый заголовок");
        changes.setHeroSubtitle("Обновлённый подзаголовок");
        changes.setPricingPayment("Оплата картой");

        service.update(changes);

        SiteSettings s = service.get();
        assertThat(s.getHeroTitle()).isEqualTo("Обновлённый заголовок");
        assertThat(s.getPricingPayment()).isEqualTo("Оплата картой");
    }

    @Test
    void update_normalizesBlankToNull() {
        SiteSettings changes = new SiteSettings();
        changes.setHeroTitle("Заголовок");
        changes.setHeroSubtitle("Подзаголовок");
        changes.setPricingTitle("   ");   // пробелы → null

        service.update(changes);

        assertThat(service.get().getPricingTitle()).isNull();
    }
}
