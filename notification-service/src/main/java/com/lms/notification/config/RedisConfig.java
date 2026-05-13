package com.lms.notification.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * RedisConfig – configures Redis-backed caching for the Notification-Service.
 *
 * <p>Each cache namespace can have its own TTL. Currently defined:
 * <ul>
 *   <li>{@code notifications} – per-user notification list, TTL = 5 minutes</li>
 * </ul>
 *
 * <p>Values are serialized as JSON (via {@link GenericJackson2JsonRedisSerializer})
 * so they are human-inspectable with {@code redis-cli}.
 *
 * @author LMS Team
 */
@Configuration
public class RedisConfig {

    /** Default TTL applied to any cache not listed in {@code cacheConfigurations}. */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    /** TTL for the notifications cache – balances freshness vs. DB load. */
    private static final Duration NOTIFICATIONS_TTL = Duration.ofMinutes(5);

    /**
     * Configures the {@link CacheManager} with per-cache TTL settings and
     * JSON serialization for both keys and values.
     *
     * @param connectionFactory Redis connection factory (auto-configured by Spring Boot)
     * @return configured cache manager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // ── Default configuration ──────────────────────────────────────────
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // ── Per-cache TTL overrides ────────────────────────────────────────
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("notifications",
                defaultConfig.entryTtl(NOTIFICATIONS_TTL));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
