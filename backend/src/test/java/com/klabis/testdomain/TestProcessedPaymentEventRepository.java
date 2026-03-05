package com.klabis.testdomain;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for ProcessedPaymentEvent (idempotency tracking).
 */
@Repository
public interface TestProcessedPaymentEventRepository extends CrudRepository<ProcessedPaymentEvent, UUID> {
}
