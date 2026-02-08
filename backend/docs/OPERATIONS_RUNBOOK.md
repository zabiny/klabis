# Operations Runbook - Spring Modulith Event Processing

This runbook provides operational guidance for monitoring and troubleshooting Spring Modulith event processing in the
Klabis Backend application.

## Table of Contents

1. [Monitoring Event Publication](#monitoring-event-publication)
2. [Investigating Failed Events](#investigating-failed-events)
3. [Manually Replaying Events](#manually-replaying-events)
4. [Common Issues and Solutions](#common-issues-and-solutions)
5. [Performance Considerations](#performance-considerations)

---

## Monitoring Event Publication

### Actuator Endpoints

Spring Modulith provides built-in monitoring via Spring Boot Actuator:

```bash
# Get event publication metrics
curl -k https://localhost:8443/actuator/modulith | jq

# Get application health (includes Modulith health indicator)
curl -k https://localhost:8443/actuator/health | jq
```

**Example Response:**

```json
{
  "applications": {
    "Klabis Membership Management": {
      "name": "Klabis Membership Management",
      "modules": [
        {
          "name": "members",
          "displayName": "Members Module"
        },
        {
          "name": "users",
          "displayName": "Users Module"
        }
      ],
      "events": [
        {
          "completed": true,
          "completedAt": "2026-01-13T10:30:45.123Z",
          "eventType": "com.klabis.members.MemberCreatedEvent",
          "listenerId": "memberCreatedEventHandler",
          "publicationDate": "2026-01-13T10:30:44.987Z"
        }
      ]
    }
  }
}
```

### Database Queries

You can directly query the `EVENT_PUBLICATION` table to check event status:

```sql
-- Check incomplete events (failed or in progress)
SELECT
    id,
    event_type,
    listener_id,
    publication_date,
    CURRENT_TIMESTAMP - publication_date AS age
FROM event_publication
WHERE completion_date IS NULL
ORDER BY publication_date DESC;

-- Check recently completed events
SELECT
    id,
    event_type,
    listener_id,
    publication_date,
    completion_date,
    completion_date - publication_date AS processing_time
FROM event_publication
WHERE completion_date IS NOT NULL
ORDER BY completion_date DESC
LIMIT 100;

-- Count events by status
SELECT
    CASE
        WHEN completion_date IS NULL THEN 'incomplete'
        ELSE 'completed'
    END AS status,
    COUNT(*) AS count
FROM event_publication
GROUP BY status;
```

### Event Logging

Enable DEBUG logging for detailed event flow tracing:

```yaml
# application.yml
logging:
  level:
    org.springframework.modulith.events: DEBUG
```

**Log Examples:**

```
2026-01-13 10:30:44.987 DEBUG [event-publication-1] o.s.m.e.EventPublicationRegistry : Publishing event MemberCreatedEvent{eventId=123e4567-...}
2026-01-13 10:30:45.123 DEBUG [event-publication-1] o.s.m.e.EventPublicationRegistry : Event MemberCreatedEvent completed for listener memberCreatedEventHandler
2026-01-13 10:30:45.456 ERROR [event-publication-2] o.s.m.e.EventPublicationRegistry : Event processing failed: java.lang.NullPointerException
```

---

## Investigating Failed Events

### Step 1: Identify Incomplete Events

```sql
-- Find incomplete events older than 5 minutes
SELECT
    id,
    event_type,
    serialized_event,
    listener_id,
    publication_date,
    CURRENT_TIMESTAMP - publication_date AS age
FROM event_publication
WHERE completion_date IS NULL
  AND publication_date < CURRENT_TIMESTAMP - INTERVAL '5 minutes'
ORDER BY publication_date ASC;
```

### Step 2: Examine Event Details

```sql
-- Get event details (deserialize the SERIALIZED_EVENT column)
SELECT
    id,
    event_type,
    listener_id,
    publication_date,
    serialized_event
FROM event_publication
WHERE id = 'event-id-here';
```

### Step 3: Check Application Logs

```bash
# Search for error logs related to event processing
grep "Event processing failed" /var/log/klabis-backend/application.log

# Search for specific event ID
grep "123e4567-89ab-cdef-0123-456789abcdef" /var/log/klabis-backend/application.log
```

### Step 4: Common Failure Causes

1. **Database Connection Issues**: Check database connectivity
2. **Email Service Unavailable**: Verify SMTP server is accessible
3. **Transaction Deadlocks**: Check for lock contention
4. **Listener Errors**: Review listener exception logs
5. **Timeout Issues**: Increase event processing timeout

---

## Manually Replaying Events

### Automatic Republishing

Spring Modulith automatically republishes incomplete events based on configuration:

```yaml
# application.yml
spring:
  modulith:
    events:
      # Republish incomplete events on application restart
      republish-outstanding-events-on-restart: false  # Default: false
```

**⚠️ WARNING**: Only enable `republish-outstanding-events-on-restart` for single-instance deployments. In multi-instance
setups, this can cause duplicate event processing.

### Manual Republishing via SQL

For manual intervention, you can make events stale by updating their publication date:

```sql
-- Make events appear stale (older than republish threshold)
UPDATE event_publication
SET publication_date = CURRENT_TIMESTAMP - INTERVAL '10 minutes'
WHERE id = 'event-id-here'
  AND completion_date IS NULL;

-- Restart application to trigger automatic republishing
```

### Programmatic Republishing

You can also create a scheduled job to republish stale events:

```java
@Component
public class EventRepublishJob {

    @Autowired
    private IncompleteEventPublications incompletePublications;

    @Scheduled(cron = "0 */10 * * * *")  // Every 10 minutes
    public void republishStaleEvents() {
        // Republish events older than 5 minutes
        incompletePublications.resubmitEventsOlderThan(Duration.ofMinutes(5));
    }
}
```

---

## Common Issues and Solutions

### Issue 1: Events Not Being Processed

**Symptoms:**

- Increasing count of incomplete events
- No listener execution in logs
- Events stuck in `PUBLISHED` state

**Diagnosis:**

```sql
-- Check for stale incomplete events
SELECT COUNT(*)
FROM event_publication
WHERE completion_date IS NULL
  AND publication_date < CURRENT_TIMESTAMP - INTERVAL '1 hour';
```

**Solutions:**

1. Check if event processing thread is running: `grep "event-publication" application.log`
2. Verify application is not shutting down or restarting
3. Check database connection pool health
4. Review listener execution errors in logs

### Issue 2: Duplicate Event Processing

**Symptoms:**

- Same event processed multiple times
- Duplicate emails sent
- Idempotency violations

**Diagnosis:**

```sql
-- Check for duplicate events (same event hash)
SELECT
    listener_id,
    serialized_event,
    COUNT(*) AS count
FROM event_publication
WHERE completion_date IS NOT NULL
GROUP BY listener_id, serialized_event
HAVING COUNT(*) > 1;
```

**Solutions:**

1. Ensure idempotency in event handlers
2. Check `@ApplicationModuleListener` configuration
3. Review transaction boundaries
4. Add deduplication logic if needed

### Issue 3: Event Table Growing Too Large

**Symptoms:**

- `EVENT_PUBLICATION` table has millions of rows
- Slow query performance
- High database storage usage

**Diagnosis:**

```sql
-- Check table size
SELECT pg_size_pretty(pg_total_relation_size('event_publication'));

-- Check event count by age
SELECT
    CASE
        WHEN completion_date < CURRENT_TIMESTAMP - INTERVAL '7 days' THEN 'old_completed'
        WHEN completion_date IS NOT NULL THEN 'recent_completed'
        ELSE 'incomplete'
    END AS category,
    COUNT(*) AS count
FROM event_publication
GROUP BY category;
```

**Solutions:**

```java
// Schedule old completed event cleanup
@Component
public class EventCleanupJob {

    @Autowired
    private CompletedEventPublications completedPublications;

    @Scheduled(cron = "0 0 2 * * *")  // 2 AM daily
    public void cleanupOldCompletedEvents() {
        // Delete completed events older than 7 days
        completedPublications.deletePublicationsOlderThan(Duration.ofDays(7));
    }
}
```

### Issue 4: High Event Processing Latency

**Symptoms:**

- Events take minutes to complete
- `processing_time` (completion_date - publication_date) is high
- Timeouts in event handlers

**Diagnosis:**

```sql
-- Average processing time by event type
SELECT
    event_type,
    AVG(completion_date - publication_date) AS avg_processing_time,
    MAX(completion_date - publication_date) AS max_processing_time,
    COUNT(*) AS count
FROM event_publication
WHERE completion_date IS NOT NULL
  AND completion_date > CURRENT_TIMESTAMP - INTERVAL '24 hours'
GROUP BY event_type
ORDER BY avg_processing_time DESC;
```

**Solutions:**

1. Optimize slow event handlers
2. Check external service latency (SMTP, APIs)
3. Increase database connection pool size
4. Review database query performance
5. Add indexes to `EVENT_PUBLICATION` table

---

## Performance Considerations

### Event Publication Rate

**Recommended Maximum**: 100-1000 events/second per instance

**Factors Affecting Performance:**

- Database connection pool size
- Event handler complexity
- External service latency
- Network bandwidth

### Monitoring Metrics

Key metrics to monitor:

```bash
# Events per second
SELECT COUNT(*) / 60 AS events_per_second
FROM event_publication
WHERE publication_date > CURRENT_TIMESTAMP - INTERVAL '1 minute';

# Average processing time (ms)
SELECT EXTRACT(EPOCH FROM AVG(completion_date - publication_date)) * 1000 AS avg_ms
FROM event_publication
WHERE completion_date IS NOT NULL
  AND publication_date > CURRENT_TIMESTAMP - INTERVAL '1 hour';

# Failed event rate
SELECT
    COUNT(CASE WHEN completion_date IS NULL THEN 1 END) * 100.0 / COUNT(*) AS failure_rate
FROM event_publication
WHERE publication_date > CURRENT_TIMESTAMP - INTERVAL '1 hour';
```

### Performance Tuning

```yaml
# application.yml
spring:
  datasource:
    hikari:
      # Increase connection pool for high event throughput
      maximum-pool-size: 20
      minimum-idle: 10

  modulith:
    events:
      # Event completion mode (UPDATE is fastest for high throughput)
      completion-mode: UPDATE
```

---

## Emergency Procedures

### Mass Event Failure Recovery

If a large number of events fail simultaneously:

1. **Identify root cause**: Check logs for systematic errors
2. **Fix the issue**: Deploy bug fix or restore external service
3. **Pause new events**: Stop affected modules if possible
4. **Batch republish**: Use SQL to make events stale gradually
5. **Monitor progress**: Watch event processing metrics

### Complete Event Table Reset (Last Resort)

⚠️ **WARNING**: This deletes all event history and should only be done in development/test environments.

```sql
-- Delete all event publications
DELETE FROM event_publication;

-- Reset sequence if applicable
-- (PostgreSQL uses UUIDs, so no sequence reset needed)
```

---

## Additional Resources

- [Spring Modulith Documentation](https://docs.spring.io/spring-modulith/reference/)
- [Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)
- [README.md](../README.md) - Project overview
- [ARCHITECTURE.md](ARCHITECTURE.md) - Architecture documentation

---

## Version History

- **v1.1** (2026-01-14) - Updated with custom metrics and event lifecycle logging
- **v1.0** (2026-01-13) - Initial version for Iteration 16
