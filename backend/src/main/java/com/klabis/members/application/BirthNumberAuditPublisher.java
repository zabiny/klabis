package com.klabis.members.application;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.UserId;
import com.klabis.members.BirthNumberAccessedEvent;
import com.klabis.members.MemberId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publishes GDPR audit events for birth number access and modification.
 *
 * <p>Annotated with {@link MvcComponent} so it is available in both full Spring contexts
 * and {@code @WebMvcTest} slices, since it only depends on {@link ApplicationEventPublisher}
 * which is always available.
 *
 * <p>Extracted as a separate bean to simplify testing — controllers depend on this instead
 * of directly wiring {@link ApplicationEventPublisher}.
 */
@MvcComponent
public class BirthNumberAuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(BirthNumberAuditPublisher.class);

    private final ApplicationEventPublisher eventPublisher;

    public BirthNumberAuditPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishViewed(UserId actingUserId, MemberId memberId) {
        log.debug("Publishing VIEW_BIRTH_NUMBER audit event: memberId={}, userId={}", memberId, actingUserId);
        eventPublisher.publishEvent(BirthNumberAccessedEvent.viewed(actingUserId, memberId));
    }

    public void publishModified(UserId actingUserId, MemberId memberId) {
        log.debug("Publishing MODIFY_BIRTH_NUMBER audit event: memberId={}, userId={}", memberId, actingUserId);
        eventPublisher.publishEvent(BirthNumberAccessedEvent.modified(actingUserId, memberId));
    }
}
