package com.soongjamm.customers;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

interface CustomerRepository extends ReactiveCrudRepository<Customer, Integer> {

}
