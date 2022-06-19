package com.soongjamm.orders;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
public class OrderController {
    private final Map<Integer, List<Order>> orders = new ConcurrentHashMap<>();

    public OrderController() {
        for (var customerId = 0; customerId <= 8; customerId++) {
            var orders = new ArrayList<Order>();

            for (int orderId = 0; orderId <= Math.random() * 100; orderId++) {
                orders.add(new Order(orderId, customerId));
            }
            this.orders.put(customerId, orders);
        }
    }

    @SneakyThrows
    @MessageMapping("orders.{customerId}")
    public Flux<Order> getFor(@DestinationVariable Integer customerId) {
        TimeUnit.SECONDS.sleep(3);
        log.info("request for {}", customerId);
        var orders = this.orders.getOrDefault(customerId, Collections.emptyList());
        return Flux.fromIterable(orders);
    }
}

