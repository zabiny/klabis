package com.klabis.notifications.infrastructure.jdbc;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
interface NotificationDeviceJdbcRepository extends CrudRepository<NotificationDeviceMemento, UUID> {
}
