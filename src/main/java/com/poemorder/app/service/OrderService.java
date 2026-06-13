package com.poemorder.app.service;

import com.poemorder.app.domain.order.Order;
import com.poemorder.app.domain.order.OrderStatus;
import com.poemorder.app.dto.OrderForm;
import com.poemorder.app.integration.telegram.TelegramMessageFormatter;
import com.poemorder.app.integration.telegram.TelegramNotifier;
import com.poemorder.app.repo.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final TelegramNotifier telegramNotifier;
    private final TelegramMessageFormatter telegramFormatter;

    public OrderService(OrderRepository orderRepository,
                        TelegramNotifier telegramNotifier,
                        TelegramMessageFormatter telegramFormatter) {
        this.orderRepository = orderRepository;
        this.telegramNotifier = telegramNotifier;
        this.telegramFormatter = telegramFormatter;
    }

    public Order createFromForm(OrderForm form) {
        Order o = new Order();
        o.setName(form.getName().trim());
        o.setPhone(form.getPhone().trim());
        o.setSocial(form.getSocial() == null ? null : form.getSocial().trim());
        o.setDescription(form.getDescription().trim());
        o.setStatus(OrderStatus.NEW);

        Order saved = orderRepository.save(o);
        log.info("New order #{} created (name='{}')", saved.getId(), saved.getName());
        telegramNotifier.send(telegramFormatter.newOrder(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Order> list(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Order get(long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public void changeStatus(long id, OrderStatus status) {
        Order o = get(id);
        OrderStatus old = o.getStatus();
        o.setStatus(status);
        log.info("Order #{} status changed {} -> {}", id, old, status);
    }
}
