package com.poemorder.app.web.admin;

import com.poemorder.app.domain.settings.ContactLink;
import com.poemorder.app.service.ContactLinkService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/contacts")
public class AdminContactLinkController {

    private final ContactLinkService service;

    public AdminContactLinkController(ContactLinkService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.adminList());
        return "admin/contacts-list";
    }

    @PostMapping("/save")
    public String save(
            @RequestParam(required = false) Long id,
            @RequestParam String label,
            @RequestParam String value,
            @RequestParam(required = false) String href,
            @RequestParam(defaultValue = "0") int sortOrder,
            // чистый паттерн чекбокса: отмечен → enabled=true; снят → параметр отсутствует → false
            @RequestParam(defaultValue = "false") boolean enabled
    ) {
        try {
            service.upsert(id, label, value, href, sortOrder, enabled);
        } catch (IllegalArgumentException e) {
            return "redirect:/admin/contacts?error";
        }
        return "redirect:/admin/contacts?ok";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/admin/contacts?deleted";
    }
}
