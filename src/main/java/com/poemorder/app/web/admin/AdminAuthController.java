package com.poemorder.app.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminAuthController {

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    @GetMapping
    public String adminRoot() {
        return "redirect:/admin/dashboard";
    }
}
