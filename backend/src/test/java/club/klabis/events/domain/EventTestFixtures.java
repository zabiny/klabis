package club.klabis.events.domain;

import club.klabis.events.domain.forms.EventRegistrationForm;
import club.klabis.finance.domain.MoneyAmount;
import club.klabis.members.MemberId;
import club.klabis.shared.config.Globals;
import org.springframework.data.domain.AggregatedRootTestUtils;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Set;

/**
 * Test fixtures and builders for Event domain testing.
 * Provides reusable test data and factory methods.
 */
class EventTestFixtures {

    // ==================== Date Constants ====================

    static final LocalDate DEFAULT_EVENT_DATE = LocalDate.now().plusDays(7);
    static final LocalDate PAST_EVENT_DATE = LocalDate.now().minusDays(7);
    static final LocalDate FUTURE_EVENT_DATE = LocalDate.now().plusDays(14);

    static final ZonedDateTime DEFAULT_DEADLINE = Globals.toZonedDateTime(DEFAULT_EVENT_DATE.minusDays(2));
    static final ZonedDateTime PAST_DEADLINE = Globals.toZonedDateTime(PAST_EVENT_DATE);
    static final ZonedDateTime FUTURE_DEADLINE = Globals.toZonedDateTime(FUTURE_EVENT_DATE.minusDays(2));

    // ==================== Member Constants ====================

    static final MemberId MEMBER_1 = new MemberId(1);
    static final MemberId MEMBER_2 = new MemberId(2);
    static final MemberId MEMBER_3 = new MemberId(3);
    static final MemberId COORDINATOR = new MemberId(100);

    // ==================== Competition Category Constants ====================

    static final Competition.Category CATEGORY_D12 = new Competition.Category("D12");
    static final Competition.Category CATEGORY_H12 = new Competition.Category("H12");
    static final Competition.Category CATEGORY_D21 = new Competition.Category("D21");
    static final Competition.Category CATEGORY_H21 = new Competition.Category("H21");
    static final Competition.Category CATEGORY_D35 = new Competition.Category("D35");

    static final Set<Competition.Category> DEFAULT_CATEGORIES = Set.of(CATEGORY_D12, CATEGORY_H21, CATEGORY_D21);

    // ==================== Other Constants ====================

    static final OrisId DEFAULT_ORIS_ID = new OrisId(12345);
    static final MoneyAmount DEFAULT_COST = MoneyAmount.of(new BigDecimal("150.00"));

    // ==================== Competition Factory Methods ====================

    /**
     * Creates a Competition with registrations open (deadline in future)
     */
    static Competition createOpenCompetition() {
        return createOpenCompetition("Test Competition", DEFAULT_EVENT_DATE);
    }

    /**
     * Creates a Competition with custom name and date, registrations open
     */
    static Competition createOpenCompetition(String name, LocalDate eventDate) {
        Competition competition = Competition.newEvent(name, eventDate, DEFAULT_CATEGORIES);
        competition.setLocation("Test Location");
        competition.setOrganizer("Test Organizer");
        return competition;
    }

    /**
     * Creates a Competition with registrations closed (deadline in past)
     */
    static Competition createClosedCompetition() {
        Competition competition = Competition.newEvent("Closed Competition", PAST_EVENT_DATE, DEFAULT_CATEGORIES);
        competition.setLocation("Past Location");
        competition.setOrganizer("Past Organizer");
        return competition;
    }

    /**
     * Creates a Competition with registrations from specified members
     */
    static Competition createCompetitionWithRegistrations(MemberId... members) {
        Competition competition = createOpenCompetition();
        clearDomainEvents(competition);

        for (MemberId memberId : members) {
            EventRegistrationForm form = defaultRegistrationForm();
            competition.registerMember(memberId, form);
        }

        return competition;
    }

    /**
     * Creates a Competition with specific categories
     */
    static Competition createCompetitionWithCategories(String... categoryNames) {
        Set<Competition.Category> categories = Set.of(categoryNames)
                .stream()
                .map(Competition.Category::new)
                .collect(java.util.stream.Collectors.toSet());

        return Competition.newEvent("Test Competition", DEFAULT_EVENT_DATE, categories);
    }

    /**
     * Creates a Competition with specific categories (Category objects)
     */
    static Competition createCompetitionWithCategories(Set<Competition.Category> categories) {
        return Competition.newEvent("Test Competition", DEFAULT_EVENT_DATE, categories);
    }

    // ==================== Registration Form Factory Methods ====================

    /**
     * Creates a default EventRegistrationForm
     */
    static EventRegistrationForm defaultRegistrationForm() {
        return createRegistrationForm(CATEGORY_D12.name(), "123456");
    }

    /**
     * Creates a custom EventRegistrationForm
     */
    static EventRegistrationForm createRegistrationForm(String category, String siNumber) {
        return new EventRegistrationForm(siNumber, category);
    }

    /**
     * Creates a registration form for a specific category
     */
    static EventRegistrationForm formForCategory(Competition.Category category) {
        return createRegistrationForm(category.name(), "123456");
    }

    // ==================== URL Utility ====================

    /**
     * Creates a test URL safely
     */
    static URL createTestUrl(String urlString) {
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid test URL: " + urlString, e);
        }
    }

    // ==================== Domain Event Utility ====================

    /**
     * Clears domain events from an Event aggregate.
     * Use this after setup to avoid polluting tests with setup events.
     */
    static void clearDomainEvents(Event event) {
        AggregatedRootTestUtils.clearDomainEvents(event);
    }
}
