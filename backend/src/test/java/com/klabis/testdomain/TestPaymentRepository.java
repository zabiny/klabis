package com.klabis.testdomain;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Payment aggregate in test domain.
 */
@Repository
public interface TestPaymentRepository extends CrudRepository<Payment, UUID> {

    @Query("SELECT * FROM test_payment WHERE order_id = :orderId")
    Optional<Payment> findByOrderId(UUID orderId);

    @Query("SELECT COUNT(*) FROM test_payment WHERE order_id = :orderId")
    int countByOrderId(UUID orderId);
}
