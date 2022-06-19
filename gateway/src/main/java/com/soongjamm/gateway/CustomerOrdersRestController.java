package com.soongjamm.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
class CustomerOrdersRestController {
    private final CrmClient crmClient;

    @GetMapping("/cos")
    Flux<CustomerOrder> get() {
        return this.crmClient.getCustomerOrders();
    }
}
