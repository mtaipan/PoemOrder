package com.poemorder.app.web;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит-тест rate-limit фильтра на spring-test mock'ах, без поднятия контекста.
 */
class RateLimitFilterTest {

    /** @return true, если запрос прошёл дальше по цепочке (не заблокирован). */
    private boolean pass(RateLimitFilter filter, String method, String uri, String ip)
            throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        if (ip != null) {
            req.addHeader("X-Forwarded-For", ip);
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        boolean blocked = res.getStatus() == 429;
        boolean chained = chain.getRequest() != null;
        // взаимоисключающе: либо заблокирован, либо пропущен дальше
        assertThat(blocked).isNotEqualTo(chained);
        return chained;
    }

    @Test
    void allowsUpToLimitThenBlocks() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2, 10);

        assertThat(pass(filter, "POST", "/order", "1.1.1.1")).isTrue();
        assertThat(pass(filter, "POST", "/order", "1.1.1.1")).isTrue();
        assertThat(pass(filter, "POST", "/order", "1.1.1.1")).isFalse(); // 3-й → 429
    }

    @Test
    void countersArePerIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 10);

        assertThat(pass(filter, "POST", "/order", "1.1.1.1")).isTrue();
        assertThat(pass(filter, "POST", "/order", "1.1.1.1")).isFalse(); // лимит для первого IP
        assertThat(pass(filter, "POST", "/order", "2.2.2.2")).isTrue();  // второй IP — свой счётчик
    }

    @Test
    void usesFirstAddressFromForwardedFor() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 10);

        assertThat(pass(filter, "POST", "/order", "9.9.9.9, 10.0.0.1")).isTrue();
        assertThat(pass(filter, "POST", "/order", "9.9.9.9, 10.0.0.2")).isFalse(); // тот же реальный клиент 9.9.9.9
    }

    @Test
    void onlyLimitsConfiguredPaths() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 10);

        // /reviews лимитируется
        assertThat(pass(filter, "POST", "/reviews", "3.3.3.3")).isTrue();
        assertThat(pass(filter, "POST", "/reviews", "3.3.3.3")).isFalse();

        // другой путь не лимитируется вовсе
        for (int i = 0; i < 5; i++) {
            assertThat(pass(filter, "POST", "/contacts", "3.3.3.3")).isTrue();
        }
    }

    @Test
    void doesNotLimitGet() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 10);

        for (int i = 0; i < 5; i++) {
            assertThat(pass(filter, "GET", "/order", "4.4.4.4")).isTrue();
        }
    }

    @Test
    void expiredWindowResetsCounter() throws Exception {
        // окно 0 минут → счётчик всегда просрочен → каждый запрос начинает новое окно
        RateLimitFilter filter = new RateLimitFilter(1, 0);

        for (int i = 0; i < 4; i++) {
            assertThat(pass(filter, "POST", "/order", "5.5.5.5")).isTrue();
        }
    }
}
