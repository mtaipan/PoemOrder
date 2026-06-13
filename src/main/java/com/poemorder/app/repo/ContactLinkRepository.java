package com.poemorder.app.repo;

import com.poemorder.app.domain.settings.ContactLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactLinkRepository extends JpaRepository<ContactLink, Long> {
    List<ContactLink> findAllByEnabledTrueOrderBySortOrderAsc();
    List<ContactLink> findAllByOrderBySortOrderAsc();
}
