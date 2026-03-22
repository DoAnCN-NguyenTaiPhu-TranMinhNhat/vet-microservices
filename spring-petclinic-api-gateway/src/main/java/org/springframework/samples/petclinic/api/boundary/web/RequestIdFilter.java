package org.springframework.samples.petclinic.api.boundary.web;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * P0 Monitoring: ensure every request has an id and propagate downstream.
 */
@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    private static final String HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String id = req.getHeaders().getFirst(HEADER);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            ServerHttpRequest mutated = req.mutate().header(HEADER, id).build();
            return chain.filter(exchange.mutate().request(mutated).build());
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100; // run early
    }
}

