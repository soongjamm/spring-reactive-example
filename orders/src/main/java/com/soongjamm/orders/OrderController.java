package com.soongjamm.orders;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class OrderController {
    private final Map<Integer, List<Order>> orders = new ConcurrentHashMap<>();

    public OrderController() {
        for (var customerId = 0; customerId < 9; customerId++) {
            var orders = new ArrayList<Order>();

            for (int orderId = 0; orderId < Math.random() * 100; orderId++) {
                orders.add(new Order(orderId, customerId));
            }
            this.orders.put(customerId, orders);
        }
    }

    @MessageMapping("orders.{customerId}")
    Flux<Order> getFor(@DestinationVariable Integer customerId) {
        var orders = this.orders.get(customerId);
        return Flux.fromIterable(orders);
    }
}

