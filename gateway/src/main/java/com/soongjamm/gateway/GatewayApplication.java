package com.soongjamm.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }


    @Bean
    RouteLocator gateway(RouteLocatorBuilder rlb) {
        return rlb
                .routes()
                .route(routeSpec ->
                                routeSpec.path("/proxy").and().host("*.spring.io")
                                        .filters(filterSpec ->
                                                        filterSpec.setPath("/customers")
                                                                .setResponseHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
//                                                        .circuitBreaker( c -> c.setFallbackUri("forward:/fallback"))
//                                                        .requestRateLimiter(rl -> rl.setRateLimiter())
                                        )
                                        .uri("http://localhost:8080") // :8080/customers/
//                                        .uri("http://localhost:8080/customers") // :8080/customers/proxy - proxy 로 들어온 요청을 customers 로 변경해줘야하는데 그러지 못함.
                )
                .build();
    }
}
