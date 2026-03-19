package com.hub.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiter en mémoire (sans Redis).
 * Limite les requêtes par IP.
 */
@Component
public class InMemoryRateLimiterGatewayFilterFactory
        extends AbstractGatewayFilterFactory<InMemoryRateLimiterGatewayFilterFactory.Config> {

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    public InMemoryRateLimiterGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String key = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";

            RateLimitBucket bucket = buckets.computeIfAbsent(key,
                    k -> new RateLimitBucket(config.getRequestsPerSecond(), config.getBurstCapacity()));

            if (!bucket.tryConsume()) {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }

    public static class Config {
        private int requestsPerSecond = 10;
        private int burstCapacity = 20;

        public int getRequestsPerSecond() {
            return requestsPerSecond;
        }

        public void setRequestsPerSecond(int requestsPerSecond) {
            this.requestsPerSecond = requestsPerSecond;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }
    }

    private static class RateLimitBucket {
        private final int replenishRate;
        private final int burstCapacity;
        private final AtomicInteger tokens;
        private final AtomicLong lastRefill;

        RateLimitBucket(int replenishRate, int burstCapacity) {
            this.replenishRate = replenishRate;
            this.burstCapacity = burstCapacity;
            this.tokens = new AtomicInteger(burstCapacity);
            this.lastRefill = new AtomicLong(System.currentTimeMillis());
        }

        synchronized boolean tryConsume() {
            refill();
            int current = tokens.get();
            if (current > 0) {
                return tokens.compareAndSet(current, current - 1);
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long last = lastRefill.get();
            long elapsed = (now - last) / 1000;
            if (elapsed > 0) {
                lastRefill.set(now);
                int toAdd = (int) Math.min(elapsed * replenishRate, burstCapacity);
                tokens.updateAndGet(t -> Math.min(burstCapacity, t + toAdd));
            }
        }
    }
}
