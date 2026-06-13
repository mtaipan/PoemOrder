package com.poemorder.app.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Простой in-memory кэш для site_settings и contact_link — они читаются
 * на каждом публичном запросе, а меняются только из админки.
 * Бин объявлен явно: в Spring Boot 4 cache-автоконфигурация живёт
 * в отдельном стартере, который мы не подключаем.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("siteSettings", "contactLinks");
    }
}
