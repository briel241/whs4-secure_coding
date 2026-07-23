package com.tinyshop.chat.service;

import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.config.AppProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class MessageRateLimiter {
    private final Map<UUID, ArrayDeque<Instant>> attempts = new ConcurrentHashMap<>();
    private final AppProperties properties;

    public MessageRateLimiter(AppProperties properties) {
        this.properties = properties;
    }

    public void check(UUID userId) {
        Instant cutoff = Instant.now().minus(Duration.ofSeconds(properties.message().rateLimitWindowSeconds()));
        ArrayDeque<Instant> queue = attempts.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && queue.peekFirst().isBefore(cutoff)) queue.removeFirst();
            if (queue.size() >= properties.message().rateLimitCount()) {
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                    "메시지를 너무 빠르게 보내고 있습니다. 잠시 후 다시 시도해 주세요.");
            }
            queue.addLast(Instant.now());
        }
    }
}
