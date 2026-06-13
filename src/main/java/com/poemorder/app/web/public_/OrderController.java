package com.poemorder.app.web.public_;

import com.poemorder.app.dto.OrderForm;
import com.poemorder.app.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/order")
    public String orderPage(Model model) {
        model.addAttribute("activePage", "order");
        if (!model.containsAttribute("orderForm")) {
            model.addAttribute("orderForm", new OrderForm());
        }
        return "public/order";
    }

    @PostMapping("/order")
    public String submitOrder(
            @Valid @ModelAttribute("orderForm") OrderForm form,
            BindingResult binding,
            RedirectAttributes ra
    ) {
        // honeypot: бот заполнил скрытое поле — показываем «успех», ничего не сохраняем
        if (form.getWebsite() != null && !form.getWebsite().isBlank()) {
            ra.addFlashAttribute("success", "Заявка отправлена!");
            return "redirect:/order";
        }

        if (binding.hasErrors()) {
            ra.addFlashAttribute("org.springframework.validation.BindingResult.orderForm", binding);
            ra.addFlashAttribute("orderForm", form);
            return "redirect:/order";
        }

        orderService.createFromForm(form);

        ra.addFlashAttribute("success", "Заявка отправлена! Я свяжусь с тобой.");
        return "redirect:/order";
    }
}
