package com.poemorder.app.web.admin;

import com.poemorder.app.domain.settings.SiteSettings;
import com.poemorder.app.service.SiteSettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    private final SiteSettingsService settingsService;

    public AdminSettingsController(SiteSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute("settings", settingsService.get());
        return "admin/settings";
    }

    @PostMapping
    public String save(@ModelAttribute("settings") SiteSettings settings) {
        settingsService.update(settings);
        return "redirect:/admin/settings?ok";
    }
}
