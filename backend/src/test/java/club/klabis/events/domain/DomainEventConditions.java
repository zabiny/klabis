package club.klabis.events.domain;

import club.klabis.events.domain.events.*;
import club.klabis.members.MemberId;
import org.assertj.core.api.Condition;

import java.time.LocalDate;

/**
 * Custom AssertJ conditions for domain event assertions.
 * Provides fluent verification of domain events published by Event aggregates.
 */
class DomainEventConditions {

    // ==================== Event Date Changed Conditions ====================

    static Condition<EventDateChangedEvent> forEvent(Event.Id eventId) {
        return new Condition<>((EventDateChangedEvent event) -> event.getAggregate().getId().equals(eventId),
                "is event date changed for event %s", eventId.value());
    }

    static Condition<EventDateChangedEvent> hasEventWithDate(LocalDate expectedDate) {
        return new Condition<>((EventDateChangedEvent event) -> event.getAggregate().getDate().equals(expectedDate),
                "has event with date %s", expectedDate);
    }

    // ==================== Event Cost Changed Conditions ====================

    static Condition<EventCostChangedEvent> forEventCost(Event.Id eventId) {
        return new Condition<>((EventCostChangedEvent event) -> event.getAggregate().getId().equals(eventId),
                "is event cost changed for event %s", eventId.value());
    }

    // ==================== Event Registrations Deadline Changed Conditions ====================

    static Condition<EventRegistrationsDeadlineChangedEvent> forEventDeadline(Event.Id eventId) {
        return new Condition<>((EventRegistrationsDeadlineChangedEvent event) ->
                event.getAggregate().getId().equals(eventId),
                "is registration deadline changed for event %s", eventId.value());
    }

    // ==================== Member Registration Created Conditions ====================

    static Condition<MemberEventRegistrationCreated> registrationCreated(Event.Id eventId, MemberId memberId) {
        return new Condition<>((MemberEventRegistrationCreated event) ->
                event.getAggregate().getId().equals(eventId) && event.getMemberId().equals(memberId),
                "is registration created for event %s and member %s", eventId.value(), memberId.value());
    }

    static Condition<MemberEventRegistrationCreated> forRegistrationCreatedEvent(Event.Id eventId) {
        return new Condition<>((MemberEventRegistrationCreated event) ->
                event.getAggregate().getId().equals(eventId),
                "is registration created for event %s", eventId.value());
    }

    static Condition<MemberEventRegistrationCreated> forMember(MemberId memberId) {
        return new Condition<>((MemberEventRegistrationCreated event) ->
                event.getMemberId().equals(memberId),
                "is registration created for member %s", memberId.value());
    }

    // ==================== Member Registration Removed Conditions ====================

    static Condition<MemberEventRegistrationRemoved> registrationRemoved(Event.Id eventId, MemberId memberId) {
        return new Condition<>((MemberEventRegistrationRemoved event) ->
                event.getAggregate().getId().equals(eventId) && event.getMemberId().equals(memberId),
                "is registration removed for event %s and member %s", eventId.value(), memberId.value());
    }

    static Condition<MemberEventRegistrationRemoved> forRegistrationRemovedEvent(Event.Id eventId) {
        return new Condition<>((MemberEventRegistrationRemoved event) ->
                event.getAggregate().getId().equals(eventId),
                "is registration removed for event %s", eventId.value());
    }

    static Condition<MemberEventRegistrationRemoved> forMemberRemoved(MemberId memberId) {
        return new Condition<>((MemberEventRegistrationRemoved event) ->
                event.getMemberId().equals(memberId),
                "is registration removed for member %s", memberId.value());
    }
}
