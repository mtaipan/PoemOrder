package com.poemorder.app.web.public_;

import com.poemorder.app.service.PoemService;
import com.poemorder.app.service.ReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PublicPageController {

    private final ReviewService reviewService;
    private final PoemService poemService;

    public PublicPageController(ReviewService reviewService, PoemService poemService) {
        this.reviewService = reviewService;
        this.poemService = poemService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("featuredReviews", reviewService.findApprovedForHomepage(3));
        model.addAttribute("featuredWorks", poemService.publishedForHomepage(3));
        model.addAttribute("activePage", "home");
        return "public/index";
    }

    @GetMapping("/contacts")
    public String contacts(Model model) {
        model.addAttribute("activePage", "contacts");
        return "public/contacts";
    }

    @GetMapping("/portfolio")
    public String portfolio(Model model) {
        model.addAttribute("items", poemService.publishedAll());
        model.addAttribute("activePage", "portfolio");
        return "public/portfolio";
    }

    @GetMapping("/portfolio/{id}")
    public String portfolioItem(@PathVariable Long id, Model model) {
        model.addAttribute("activePage", "portfolio");
        model.addAttribute("item", poemService.getOrThrow(id));
        return "public/portfolio-item";
    }

    @GetMapping("/pricing")
    public String pricing(Model model) {
        model.addAttribute("activePage", "pricing");
        return "public/pricing";
    }
}
