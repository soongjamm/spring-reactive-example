package com.soongjamm.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CustomerOrdersRestController {
    private final CrmClient crmClient;

    @GetMapping("/cos")
    public Flux<CustomerOrder> get() {
        Flux<CustomerOrder> customerOrders = crmClient.getCustomerOrders();
        log.info("Done with get CustomerOrder Request || {} ", Thread.currentThread().getName());
        return customerOrders;
    }

    @GetMapping("/")
    public String health() {
        return "ok";
    }
}
