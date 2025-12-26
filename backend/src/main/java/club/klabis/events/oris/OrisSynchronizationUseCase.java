package club.klabis.events.oris;

import club.klabis.events.application.EventsRepository;
import club.klabis.events.application.PreferencesRepository;
import club.klabis.events.domain.Competition;
import club.klabis.events.domain.Event;
import club.klabis.events.domain.OrisId;
import club.klabis.events.domain.forms.EventRegistrationForm;
import club.klabis.events.domain.forms.EventRegistrationFormBuilder;
import club.klabis.events.oris.dto.OrisData;
import club.klabis.events.oris.dto.OrisEventListFilter;
import club.klabis.members.MemberId;
import club.klabis.members.domain.RegistrationNumber;
import club.klabis.shared.application.OrisIntegrationComponent;
import club.klabis.shared.config.ddd.UseCase;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Optional;

@OrisIntegrationComponent
@UseCase
public class OrisSynchronizationUseCase implements OrisEventSynchronizationUseCase {

    private final OrisEventDataSource orisEventDataSource;
    private final EventsRepository eventsRepository;
    private final PreferencesRepository eventsPreferencesRepository;

    private static final Logger LOG = LoggerFactory.getLogger(OrisSynchronizationUseCase.class);

    public OrisSynchronizationUseCase(OrisEventDataSource orisEventDataSource, EventsRepository eventsRepository, PreferencesRepository eventsPreferencesRepository) {
        this.orisEventDataSource = orisEventDataSource;
        this.eventsRepository = eventsRepository;
        this.eventsPreferencesRepository = eventsPreferencesRepository;
    }

    public Collection<OrisId> getOrisIds(Collection<Event.Id> eventIds) {
        return eventIds.stream()
                .map(eventsRepository::findById)
                .flatMap(Optional::stream)
                .filter(Event::hasOrisId)
                .map(Event::getOrisId)
                .flatMap(Optional::stream)
                .toList();
    }

    public void synchronizeOrisEvent(@Valid OrisData orisData) {
        Event updatedEvent = eventsRepository.findByOrisId(orisData.orisId())
                .map(event -> this.updateEvent(orisData, event))
                .orElse(this.createNewEvent(orisData));

        eventsRepository.save(updatedEvent);
    }

    private Competition createNewEvent(OrisData orisData) {
        Competition result = new Competition(orisData.name(), orisData.eventDate());
        result.linkWithOris(orisData.orisId());
        updateEvent(orisData, result);
        return result;
    }


    private Event updateEvent(OrisData orisData, Event event) {
        Assert.isTrue(orisData.orisId().equals(event.getOrisId().orElse(null)),
                "Attempt to synchronize OrisData into unexpected event - importing oris event with id %s into klabis event with orisId %s".formatted(
                        orisData.orisId(),
                        event.getOrisId().orElse(null)));

        event.setName(orisData.name());
        event.setLocation(orisData.location());
        event.setOrganizer(orisData.organizer());
        event.setEventDate(orisData.eventDate());
        event.setRegistrationDeadline(orisData.registrationsDeadline());
        event.withWebsite(orisData.website());

        orisData.registrations().forEach(reg -> this.synchronizeRegistration(event, reg));

        if (event instanceof Competition competition) {
            competition.setCategories(orisData.categories().stream().map(Competition.Category::new).toList());
        }

        return event;
    }

    private void synchronizeRegistration(Event targetEvent, OrisData.MemberRegistration registration) {
        eventsPreferencesRepository.findByRegistrationNumber(RegistrationNumber.ofRegistrationId(registration.memberRegistration()))
                .ifPresentOrElse(
                        memberEventsPreferences -> {
                            this.synchronizeRegistration(targetEvent,
                                    memberEventsPreferences.getMemberId(),
                                    registration);
                        },
                        () -> LOG.info(
                                "Can't synchronize registration for event {} and member {} -> member preferences doesn't exist",
                                targetEvent.getOrisId().map(OrisId::value).orElse(null),
                                registration.memberRegistration()));

    }

    private void synchronizeRegistration(Event targetEvent, MemberId memberId, OrisData.MemberRegistration registration) {
        EventRegistrationForm form = EventRegistrationFormBuilder.builder().category(registration.category()).siNumber(
                Integer.toString(registration.siCard())).build();
        if (targetEvent.isMemberRegistered(memberId)) {
            targetEvent.changeRegistration(memberId, form);
        } else {
            targetEvent.registerMember(memberId, form);
        }
    }

    @Override
    public void loadOrisEvents(OrisEventListFilter filter) {
        orisEventDataSource.streamOrisEvents(filter).forEach(this::synchronizeOrisEvent);
    }

    @Override
    public void synchronizeEvents(Collection<Event.Id> eventIds) {
        getOrisIds(eventIds).stream()
                .map(orisEventDataSource::getOrisEventData)
                .flatMap(Optional::stream)
                .forEach(this::synchronizeOrisEvent);
    }
}
