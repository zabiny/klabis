package club.klabis.events.domain;

import club.klabis.finance.domain.MoneyAmount;
import club.klabis.members.MemberId;
import org.assertj.core.api.Condition;

import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom AssertJ conditions for Event and Competition assertions.
 * Provides fluent, readable test assertions following project patterns.
 */
class EventConditions {

    // ==================== Identity Conditions ====================

    static Condition<Event> hasId(int expectedValue) {
        return hasId(new Event.Id(expectedValue));
    }

    static Condition<Event> hasId(Event.Id expectedId) {
        return new Condition<>((Event event) -> event.getId().equals(expectedId),
                "has id %s", expectedId.value());
    }

    // ==================== Basic Property Conditions ====================

    static Condition<Event> hasName(String expectedName) {
        return new Condition<>((Event event) -> expectedName.equals(event.getName()),
                "has name '%s'", expectedName);
    }

    static Condition<Event> hasLocation(String expectedLocation) {
        return new Condition<>((Event event) -> expectedLocation.equals(event.getLocation()),
                "has location '%s'", expectedLocation);
    }

    static Condition<Event> hasOrganizer(String expectedOrganizer) {
        return new Condition<>((Event event) -> expectedOrganizer.equals(event.getOrganizer()),
                "has organizer '%s'", expectedOrganizer);
    }

    static Condition<Event> hasEventDate(LocalDate expectedDate) {
        return new Condition<>((Event event) -> expectedDate.equals(event.getDate()),
                "has event date %s", expectedDate);
    }

    static Condition<Event> hasRegistrationDeadline(ZonedDateTime expectedDeadline) {
        return new Condition<>((Event event) -> expectedDeadline.equals(event.getRegistrationDeadline()),
                "has registration deadline %s", expectedDeadline);
    }

    // ==================== Optional Field Conditions ====================

    static Condition<Event> hasCoordinator(MemberId expectedCoordinator) {
        return new Condition<>((Event event) -> event.getCoordinator().isPresent()
                                                && event.getCoordinator().get().equals(expectedCoordinator),
                "has coordinator with ID %s", expectedCoordinator.value());
    }

    static Condition<Event> hasNoCoordinator() {
        return new Condition<>((Event event) -> event.getCoordinator().isEmpty(),
                "has no coordinator");
    }

    static Condition<Event> hasOrisId(int expectedOrisIdValue) {
        return hasOrisId(new OrisEventId(expectedOrisIdValue));
    }

    static Condition<Event> hasOrisId(OrisEventId expectedOrisId) {
        return new Condition<>((Event event) -> event.getOrisId().isPresent()
                                                && event.getOrisId().get().equals(expectedOrisId),
                "has ORIS ID %s", expectedOrisId.value());
    }

    static Condition<Event> hasNoOrisId() {
        return new Condition<>((Event event) -> event.getOrisId().isEmpty(),
                "has no ORIS ID");
    }

    static Condition<Event> hasWebsite(URL expectedWebsite) {
        return new Condition<>((Event event) -> event.getWebsite().isPresent()
                                                && event.getWebsite().get().equals(expectedWebsite),
                "has website %s", expectedWebsite);
    }

    static Condition<Event> hasNoWebsite() {
        return new Condition<>((Event event) -> event.getWebsite().isEmpty(),
                "has no website");
    }

    static Condition<Event> hasCost(MoneyAmount expectedCost) {
        return new Condition<>((Event event) -> event.getCost().isPresent()
                                                && event.getCost().get().equals(expectedCost),
                "has cost %s", expectedCost);
    }

    static Condition<Event> hasNoCost() {
        return new Condition<>((Event event) -> event.getCost().isEmpty(),
                "has no cost (null or zero)");
    }

    // ==================== Registration State Conditions ====================

    static Condition<Event> hasRegistrationsOpen() {
        return new Condition<>((Event event) -> event.areRegistrationsOpen(),
                "has registrations open");
    }

    static Condition<Event> hasRegistrationsClosed() {
        return new Condition<>((Event event) -> !event.areRegistrationsOpen(),
                "has registrations closed");
    }

    static Condition<Event> hasRegistrationCount(int expectedCount) {
        return new Condition<>((Event event) -> event.getEventRegistrations().size() == expectedCount,
                "has %d registration(s)", expectedCount);
    }

    static Condition<Event> hasMemberRegistered(MemberId memberId) {
        return new Condition<>((Event event) -> event.isMemberRegistered(memberId),
                "has member %s registered", memberId.value());
    }

    static Condition<Event> hasMemberNotRegistered(MemberId memberId) {
        return new Condition<>((Event event) -> !event.isMemberRegistered(memberId),
                "does not have member %s registered", memberId.value());
    }

    // ==================== Competition-Specific Conditions ====================

    static Condition<Competition> hasCategories(Competition.Category... expectedCategories) {
        Set<Competition.Category> expectedSet = Set.of(expectedCategories);
        return new Condition<>((Competition competition) ->
                competition.getCategories().size() == expectedCategories.length
                && competition.getCategories().containsAll(expectedSet),
                "has categories %s",
                Arrays.stream(expectedCategories)
                        .map(Competition.Category::name)
                        .collect(Collectors.joining(", ")));
    }

    static Condition<Competition> hasCategoryCount(int expectedCount) {
        return new Condition<>((Competition competition) -> competition.getCategories().size() == expectedCount,
                "has %d category/categories", expectedCount);
    }

    static Condition<Competition> hasCategory(String categoryName) {
        return hasCategory(new Competition.Category(categoryName));
    }

    static Condition<Competition> hasCategory(Competition.Category expectedCategory) {
        return new Condition<>((Competition competition) -> competition.getCategories().contains(expectedCategory),
                "has category '%s'", expectedCategory.name());
    }

    static Condition<Competition> doesNotHaveCategory(String categoryName) {
        return doesNotHaveCategory(new Competition.Category(categoryName));
    }

    static Condition<Competition> doesNotHaveCategory(Competition.Category category) {
        return new Condition<>((Competition competition) -> !competition.getCategories().contains(category),
                "does not have category '%s'", category.name());
    }
}
