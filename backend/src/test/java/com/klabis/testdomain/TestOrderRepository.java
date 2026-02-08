package com.klabis.testdomain;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Order aggregate in test domain.
 */
public interface TestOrderRepository extends CrudRepository<Order, UUID> {

    @Query("SELECT * FROM test_order WHERE order_number = :orderNumber")
    Optional<Order> findByOrderNumber(String orderNumber);
}
