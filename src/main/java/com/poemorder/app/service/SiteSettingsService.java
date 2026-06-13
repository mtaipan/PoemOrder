package com.poemorder.app.service;

import com.poemorder.app.domain.settings.SiteSettings;
import com.poemorder.app.repo.SiteSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class SiteSettingsService {

    public static final short SINGLETON_ID = 1;

    private static final Logger log = LoggerFactory.getLogger(SiteSettingsService.class);

    private final SiteSettingsRepository repo;

    public SiteSettingsService(SiteSettingsRepository repo) {
        this.repo = repo;
    }

    @Cacheable("siteSettings")
    @Transactional(readOnly = true)
    public SiteSettings get() {
        return repo.findById(SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("site_settings row not found (id=1). Check Flyway migration."));
    }

    @CacheEvict(value = "siteSettings", allEntries = true)
    @Transactional
    public void update(SiteSettings updated) {
        // ВАЖНО: читаем из репозитория напрямую, а не через this.get() —
        // нужна свежая managed-сущность, а не закэшированная копия
        SiteSettings s = repo.findById(SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("site_settings row not found (id=1)."));

        s.setHeroTitle(updated.getHeroTitle());
        s.setHeroSubtitle(updated.getHeroSubtitle());

        // portfolio texts
        s.setPortfolioTitle(emptyToNull(updated.getPortfolioTitle()));
        s.setPortfolioSubtitle(emptyToNull(updated.getPortfolioSubtitle()));

        // pricing / terms
        s.setPricingTitle(emptyToNull(updated.getPricingTitle()));
        s.setPricingPayment(emptyToNull(updated.getPricingPayment()));
        s.setPricingDelivery(emptyToNull(updated.getPricingDelivery()));
        s.setPricingRefund(emptyToNull(updated.getPricingRefund()));

        // legacy contacts (можешь удалить позже)
        s.setTelegram(emptyToNull(updated.getTelegram()));
        s.setPhone(emptyToNull(updated.getPhone()));
        s.setEmail(emptyToNull(updated.getEmail()));
        s.setSocial(emptyToNull(updated.getSocial()));

        s.setUpdatedAt(Instant.now());
        repo.save(s);
        log.info("Site settings updated");
    }

    private String emptyToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
