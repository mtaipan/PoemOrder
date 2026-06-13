package com.poemorder.app.web.admin;

import com.poemorder.app.service.ReviewService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/reviews")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    private static final int PAGE_SIZE = 20;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("activeTab", "reviews");
        // очередь модерации показываем целиком, пагинируем только архив опубликованных
        model.addAttribute("pending", reviewService.getPending());
        model.addAttribute("approved", reviewService.getApproved(PageRequest.of(Math.max(0, page), PAGE_SIZE)));
        return "admin/reviews-list";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        reviewService.approve(id);
        ra.addFlashAttribute("okMessage", "Отзыв опубликован.");
        return "redirect:/admin/reviews";
    }

    @PostMapping("/{id}/hide-contact")
    public String hideContact(@PathVariable Long id, RedirectAttributes ra) {
        reviewService.hideContact(id);
        ra.addFlashAttribute("okMessage", "Контакт скрыт.");
        return "redirect:/admin/reviews";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        reviewService.delete(id);
        ra.addFlashAttribute("okMessage", "Отзыв удалён.");
        return "redirect:/admin/reviews";
    }
}
