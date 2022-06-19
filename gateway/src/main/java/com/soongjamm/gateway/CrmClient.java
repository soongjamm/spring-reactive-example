package com.soongjamm.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// API Gateway 가 할수 있는 일
// - service orchestration and composition
// client 가 있고, order service, customer service 과 통신
@Slf4j
@Component
@RequiredArgsConstructor
public class CrmClient {

    private final WebClient http;
    private final RSocketRequester rSocket;

    Flux<CustomerOrder> getCustomerOrders() {
        // composition of customer data and order data
        log.info("getCustomerOrders || {}", Thread.currentThread().getName());
        return getCustomers()
                .flatMap(cu ->
                        // 모든 고객의 주문을 얻기위해 계속 Customer 참조를 가지고 있어야 함.
                        // 그리고 서로 다른 타입의 객체를 저장하기 위한 Tuple 을 만든다
                        Mono.zip(Mono.just(cu), getOrdersForCustomer(cu.getId()).collectList())
                )
                .map(tuple -> new CustomerOrder(tuple.getT1(), tuple.getT2()));
    }

    Flux<Customer> getCustomers() {
        log.info("getCustomers || {}", Thread.currentThread().getName());
        return this.http.get().uri("http://localhost:8080/customers")
                .retrieve()
                .bodyToFlux(Customer.class)
                .retry()
                .onErrorResume(ex -> Flux.empty());
        // 실패나 다운되면? retry 등 견고한 코드를 짜기 위한 다양한 것들을 reactor 가 제공함
    }

    Flux<Order> getOrdersForCustomer(Integer customerId) {
        log.info("getOrdersForCustomer - customerId={} || {}",
                customerId, Thread.currentThread().getName());
        return rSocket
                .route("orders.{customerId}", customerId)
                .retrieveFlux(Order.class);
    }
}
