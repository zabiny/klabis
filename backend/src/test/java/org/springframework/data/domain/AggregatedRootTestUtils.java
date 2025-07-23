
package org.springframework.data.domain;

import club.klabis.common.domain.DomainEvent;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AggregatedRootTestUtils {

    public static AbstractListAssert<?, List<? extends DomainEvent>, DomainEvent, ObjectAssert<DomainEvent>> assertThatDomainEventsOf(AbstractAggregateRoot<?> aggregateRoot) {
        return assertThat(aggregateRoot.domainEvents())
                .map(DomainEvent.class::cast);
    }

    public static <T extends DomainEvent> AbstractListAssert<?, List<? extends T>, T, ObjectAssert<T>> assertThatDomainEventsOf(AbstractAggregateRoot<?> aggregateRoot, Class<T> eventType) {
        List<T> eventsWithExpectedType = aggregateRoot.domainEvents().stream().filter(eventType::isInstance).map(eventType::cast).toList();
        return assertThat(eventsWithExpectedType);
    }

    /**
     * Method can be used to clear domain events before test is executed on aggregate root so events possibly added while setting up aggregate root for test are not affecting results of test
     * @param aggregateRoot
     */
    public static void clearDomainEvents(AbstractAggregateRoot<?> aggregateRoot) {
        aggregateRoot.clearDomainEvents();
    }

}
