package com.poemorder.app.web;

import com.poemorder.app.domain.order.Order;
import com.poemorder.app.domain.order.OrderStatus;
import com.poemorder.app.repo.OrderRepository;
import com.poemorder.app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AdminWebTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    OrderRepository orderRepository;

    private Order seedOrder() {
        Order o = new Order();
        o.setName("Клиент");
        o.setPhone("+79990001122");
        o.setDescription("описание");
        o.setStatus(OrderStatus.NEW);
        return orderRepository.save(o);
    }

    @Test
    void adminPagesRedirectToLoginWhenAnonymous() throws Exception {
        for (String path : new String[]{"/admin/dashboard", "/admin/orders", "/admin/poems",
                "/admin/reviews", "/admin/contacts", "/admin/settings"}) {
            mvc.perform(get(path))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/login"));
        }
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mvc.perform(get("/admin/login")).andExpect(status().isOk());
    }

    @Test
    void adminPagesAccessibleForAdmin() throws Exception {
        for (String path : new String[]{"/admin/dashboard", "/admin/orders", "/admin/poems",
                "/admin/reviews", "/admin/contacts", "/admin/settings"}) {
            mvc.perform(get(path).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void orderView_rendersAllStatusOptions() throws Exception {
        Order order = seedOrder();

        // регресс на баг allStatuses→statuses: dropdown должен содержать все статусы
        mvc.perform(get("/admin/orders/" + order.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NEW")))
                .andExpect(content().string(containsString("IN_PROGRESS")))
                .andExpect(content().string(containsString("DONE")))
                .andExpect(content().string(containsString("CANCELED")));
    }

    @Test
    void changeStatus_persistsNewStatus() throws Exception {
        Order order = seedOrder();

        mvc.perform(post("/admin/orders/" + order.getId() + "/status")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("status", "DONE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders/" + order.getId()));

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.DONE);
    }

    @Test
    void adminAction_withoutCsrfIsForbidden() throws Exception {
        Order order = seedOrder();

        mvc.perform(post("/admin/orders/" + order.getId() + "/status")
                        .with(user("admin").roles("ADMIN"))
                        .param("status", "DONE"))
                .andExpect(status().isForbidden());
    }
}
