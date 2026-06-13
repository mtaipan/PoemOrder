package com.poemorder.app.service;

import com.poemorder.app.domain.order.Order;
import com.poemorder.app.domain.order.OrderStatus;
import com.poemorder.app.dto.OrderForm;
import com.poemorder.app.repo.OrderRepository;
import com.poemorder.app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class OrderServiceTest extends AbstractIntegrationTest {

    @Autowired
    OrderService orderService;
    @Autowired
    OrderRepository orderRepository;

    private OrderForm form(String name) {
        OrderForm f = new OrderForm();
        f.setName(name);
        f.setPhone("+79990001122");
        f.setSocial("@nick");
        f.setDescription("Хочу стих");
        return f;
    }

    @Test
    void createFromForm_persistsWithNewStatusAndTrimsFields() {
        Order saved = orderService.createFromForm(form("  Иван  "));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(saved.getCreatedAt()).isNotNull();

        Order reloaded = orderRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Иван"); // trim
        assertThat(reloaded.getPhone()).isEqualTo("+79990001122");
    }

    @Test
    void changeStatus_updatesPersistedOrder() {
        Order saved = orderService.createFromForm(form("Пётр"));

        orderService.changeStatus(saved.getId(), OrderStatus.IN_PROGRESS);

        assertThat(orderRepository.findById(saved.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.IN_PROGRESS);
    }

    @Test
    void get_throwsNotFoundForMissingId() {
        assertThatThrownBy(() -> orderService.get(987654321L))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void list_isPaginatedAndSortable() {
        for (int i = 0; i < 5; i++) {
            orderService.createFromForm(form("Order-" + i));
        }

        Page<Order> page = orderService.list(
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(5);
        assertThat(page.getTotalPages()).isGreaterThanOrEqualTo(3);
    }
}
