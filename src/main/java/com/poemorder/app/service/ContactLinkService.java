package com.poemorder.app.service;

import com.poemorder.app.domain.settings.ContactLink;
import com.poemorder.app.repo.ContactLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class ContactLinkService {

    private static final Logger log = LoggerFactory.getLogger(ContactLinkService.class);

    // href уходит в th:href на публичной странице — разрешаем только безопасные схемы,
    // иначе через админку можно сохранить javascript:-ссылку (stored XSS)
    private static final Pattern ALLOWED_HREF =
            Pattern.compile("^(https?://|mailto:|tel:).+", Pattern.CASE_INSENSITIVE);

    private final ContactLinkRepository repo;

    public ContactLinkService(ContactLinkRepository repo) {
        this.repo = repo;
    }

    @Cacheable("contactLinks")
    @Transactional(readOnly = true)
    public List<ContactLink> publicList() {
        return repo.findAllByEnabledTrueOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<ContactLink> adminList() {
        return repo.findAllByOrderBySortOrderAsc();
    }

    @CacheEvict(value = "contactLinks", allEntries = true)
    @Transactional
    public ContactLink upsert(Long id,
                              String label,
                              String value,
                              String href,
                              int sortOrder,
                              boolean enabled) {

        ContactLink entity = (id == null)
                ? new ContactLink()
                : repo.findById(id).orElseThrow(() -> new IllegalArgumentException("ContactLink not found: " + id));

        entity.setLabel(label);
        entity.setValue(value);
        entity.setHref(normalizeHref(href));
        entity.setSortOrder(sortOrder);
        entity.setEnabled(enabled);

        ContactLink saved = repo.save(entity);
        log.info("Contact link #{} {} (label='{}')", saved.getId(), id == null ? "created" : "updated", label);
        return saved;
    }

    @CacheEvict(value = "contactLinks", allEntries = true)
    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
        log.info("Contact link #{} deleted", id);
    }

    private String normalizeHref(String href) {
        if (href == null || href.isBlank()) return null;
        String trimmed = href.trim();
        if (!ALLOWED_HREF.matcher(trimmed).matches()) {
            log.warn("Rejected contact link href with disallowed scheme: '{}'", trimmed);
            throw new IllegalArgumentException(
                    "Ссылка должна начинаться с http://, https://, mailto: или tel:");
        }
        return trimmed;
    }
}
