package com.soongjamm.customers;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class CustomersApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomersApplication.class, args);
    }

}

@Component
@RequiredArgsConstructor
class Initializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final DatabaseClient dbc;

    @Override
    public void run(String... args) throws Exception {
        var ddl = dbc.sql("create table customer(id serial primary key, name varchar(255) not null)")
                .fetch()
                .rowsUpdated();

        Flux<Customer> names = Flux.just("Kim", "Lee", "Park", "Choi", "Yoon", "Jang", "Noh", "Oh", "Wang")
                .map(name -> new Customer(null, name))
                .flatMap(customerRepository::save);
//                .map(customerRepository::save); // return Flux<Mono<Customer>>

        Flux<Customer> all = customerRepository.findAll();

        ddl // 세가지 다른 일이 있는데, 이 작업들은 순서가 있어야 한다. 하지만 구독 항목들의 순서를 보장할 수 없다. 그래서 thenMany 를 이용해 순서를 지정한다.
                .thenMany(names)
                .thenMany(all)
                .subscribe(System.out::println);
    }

}

