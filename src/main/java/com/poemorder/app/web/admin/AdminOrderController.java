package com.poemorder.app.web.admin;

import com.poemorder.app.domain.order.Order;
import com.poemorder.app.domain.order.OrderStatus;
import com.poemorder.app.service.OrderService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    private static final int PAGE_SIZE = 20;

    @GetMapping("/orders")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("orders", orderService.list(
                PageRequest.of(Math.max(0, page), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))));
        model.addAttribute("activeTab", "orders");
        return "admin/orders-list";
    }

    @GetMapping("/orders/{id}")
    public String view(@PathVariable long id, Model model) {
        Order order = orderService.get(id);
        model.addAttribute("order", order);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("activeTab", "orders");
        return "admin/order-view";
    }

    @PostMapping("/orders/{id}/status")
    public String updateStatus(
            @PathVariable long id,
            @RequestParam("status") OrderStatus status,
            RedirectAttributes ra
    ) {
        orderService.changeStatus(id, status);
        ra.addFlashAttribute("ok", "Статус обновлён: " + status);
        return "redirect:/admin/orders/" + id;
    }
}
