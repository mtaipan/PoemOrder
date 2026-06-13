package com.poemorder.app.repo;

import com.poemorder.app.domain.settings.SiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteSettingsRepository extends JpaRepository<SiteSettings, Short> {
}
