package com.poemorder.app.web.public_;

import com.poemorder.app.domain.settings.ContactLink;
import com.poemorder.app.domain.settings.SiteSettings;
import com.poemorder.app.service.ContactLinkService;
import com.poemorder.app.service.SiteSettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice(basePackages = "com.poemorder.app.web.public_")
public class GlobalModelAttributes {

    private final SiteSettingsService settingsService;
    private final ContactLinkService contactLinkService;
    private final String authorName;

    public GlobalModelAttributes(SiteSettingsService settingsService,
                                 ContactLinkService contactLinkService,
                                 @Value("${app.author-name:Автор}") String authorName) {
        this.settingsService = settingsService;
        this.contactLinkService = contactLinkService;
        this.authorName = authorName;
    }

    @ModelAttribute("settings")
    public SiteSettings settings() {
        return settingsService.get();
    }

    /**
     * Список контактных карточек для публичных страниц.
     * Используй в шаблонах: th:each="c : ${contactLinks}"
     */
    @ModelAttribute("contactLinks")
    public List<ContactLink> contactLinks() {
        return contactLinkService.publicList();
    }

    // совместимость со старым layout (если где-то ещё используется)
    @ModelAttribute("siteName")
    public String siteName(@ModelAttribute("settings") SiteSettings s) {
        return s.getHeroTitle();
    }

    @ModelAttribute("authorName")
    public String authorName() {
        return authorName;
    }

    @ModelAttribute("footerNote")
    public String footerNote(@ModelAttribute("settings") SiteSettings s) {
        return s.getHeroTitle();
    }
}
