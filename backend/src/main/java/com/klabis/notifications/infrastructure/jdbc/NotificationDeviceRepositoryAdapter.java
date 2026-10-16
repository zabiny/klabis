package com.klabis.notifications.infrastructure.jdbc;

import com.klabis.notifications.domain.NotificationDevice;
import com.klabis.notifications.domain.NotificationDeviceFilter;
import com.klabis.notifications.domain.NotificationDeviceRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;

import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class NotificationDeviceRepositoryAdapter implements NotificationDeviceRepository {

    private final NotificationDeviceJdbcRepository jdbcRepository;
    private final JdbcAggregateTemplate jdbcAggregateTemplate;

    NotificationDeviceRepositoryAdapter(NotificationDeviceJdbcRepository jdbcRepository,
                                        JdbcAggregateTemplate jdbcAggregateTemplate) {
        this.jdbcRepository = jdbcRepository;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
    }

    @Override
    public NotificationDevice save(NotificationDevice device) {
        return jdbcRepository.save(NotificationDeviceMemento.from(device)).toNotificationDevice();
    }

    @Override
    public List<NotificationDevice> findAll(NotificationDeviceFilter filter) {
        return jdbcAggregateTemplate.findAll(toQuery(filter), NotificationDeviceMemento.class).stream()
                .map(NotificationDeviceMemento::toNotificationDevice)
                .toList();
    }

    @Override
    public Optional<NotificationDevice> findOne(NotificationDeviceFilter filter) {
        return jdbcAggregateTemplate.findOne(toQuery(filter), NotificationDeviceMemento.class)
                .map(NotificationDeviceMemento::toNotificationDevice);
    }

    private static Query toQuery(NotificationDeviceFilter filter) {
        Criteria criteria = Criteria.empty();
        if (filter.owner() != null) {
            criteria = criteria.and("user_id").is(filter.owner().uuid());
        }
        if (filter.endpoint() != null) {
            criteria = criteria.and("endpoint").is(filter.endpoint().url());
        }
        return Query.query(criteria).sort(org.springframework.data.domain.Sort.by("enabled_at"));
    }
}
