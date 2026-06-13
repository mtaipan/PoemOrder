package com.poemorder.app.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Лимит отправок публичных форм: не более maxRequests POST-запросов
 * с одного IP за окно windowMinutes. Счётчики в памяти процесса.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of("/order", "/reviews");
    // защита от распухания карты при спаме с множества IP
    private static final int MAX_TRACKED_IPS = 10_000;

    private final int maxRequests;
    private final Duration window;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.rate-limit.max-requests:5}") int maxRequests,
            @Value("${app.rate-limit.window-minutes:10}") int windowMinutes
    ) {
        this.maxRequests = maxRequests;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (allow(clientIp(request))) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType("text/plain; charset=UTF-8");
        response.getOutputStream().write(
                "Слишком много запросов. Попробуй ещё раз позже.".getBytes(StandardCharsets.UTF_8));
    }

    private boolean allow(String ip) {
        Instant now = Instant.now();

        if (counters.size() > MAX_TRACKED_IPS) {
            counters.entrySet().removeIf(e -> e.getValue().expired(now, window));
        }

        WindowCounter counter = counters.compute(ip, (k, existing) ->
                (existing == null || existing.expired(now, window)) ? new WindowCounter(now) : existing);

        return counter.count.incrementAndGet() <= maxRequests;
    }

    private String clientIp(HttpServletRequest request) {
        // за reverse proxy реальный клиент — первый адрес в X-Forwarded-For
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record WindowCounter(Instant start, AtomicInteger count) {
        WindowCounter(Instant start) {
            this(start, new AtomicInteger());
        }

        boolean expired(Instant now, Duration window) {
            return start.plus(window).isBefore(now);
        }
    }
}
