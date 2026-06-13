package com.poemorder.app.web.public_;

import com.poemorder.app.dto.ReviewForm;
import com.poemorder.app.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReviewPublicController {

    private final ReviewService reviewService;

    public ReviewPublicController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/reviews")
    public String reviews(Model model) {
        model.addAttribute("activePage", "reviews");
        model.addAttribute("reviews", reviewService.getApproved());
        model.addAttribute("reviewForm", new ReviewForm());
        return "public/reviews";
    }

    @PostMapping("/reviews")
    public String submitReview(
            @Valid @ModelAttribute("reviewForm") ReviewForm form,
            BindingResult br,
            Model model,
            RedirectAttributes ra
    ) {
        // honeypot (если поле есть в форме) — антибот
        if (form.getWebsite() != null && !form.getWebsite().isBlank()) {
            // притворяемся успехом, чтобы боты не поняли
            ra.addFlashAttribute("okMessage", "Отзыв отправлен на модерацию.");
            return "redirect:/reviews";
        }

        if (br.hasErrors()) {
            model.addAttribute("activePage", "reviews");
            model.addAttribute("reviews", reviewService.getApproved());
            return "public/reviews";
        }

        reviewService.createPending(form);

        ra.addFlashAttribute("okMessage", "Спасибо! Отзыв отправлен на модерацию.");
        return "redirect:/reviews";
    }
}
