package com.klabis.events.infrastructure.bootstrap;

import com.klabis.common.bootstrap.BootstrapDataInitializer;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventFilter;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.SiCardNumber;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Order(3)
class EventsDataBootstrap implements BootstrapDataInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(EventsDataBootstrap.class);

    private final EventRepository eventRepository;
    private final Members members;

    EventsDataBootstrap(EventRepository eventRepository, Members members) {
        this.eventRepository = eventRepository;
        this.members = members;
    }

    @Override
    public boolean requiresBootstrap() {
        return eventRepository.findAll(EventFilter.none(), PageRequest.of(0, 1)).isEmpty();
    }

    @Override
    public void bootstrapData() {
        LocalDate today = LocalDate.now();

        MemberId coordinator = members.findByRegistrationNumber("ZBM9000")
                .map(m -> new MemberId(m.memberId()))
                .orElse(null);
        MemberId regularMember = members.findByRegistrationNumber("ZBM9500")
                .map(m -> new MemberId(m.memberId()))
                .orElse(null);

        if (coordinator == null) {
            LOG.warn("Bootstrap coordinator (ZBM9000) not found — creating events without coordinator");
        }
        if (regularMember == null) {
            LOG.warn("Bootstrap member (ZBM9500) not found — skipping registrations");
        }

        createDraftEvents(today, coordinator);
        createActiveEvents(today, coordinator, regularMember);

        LOG.info("Created 5 DRAFT and 22 ACTIVE bootstrap events");
    }

    private void createDraftEvents(LocalDate today, MemberId coordinator) {
        eventRepository.save(Event.create(new Event.CreateEvent(
                "Oblastní přebor – jarní kolo",
                today.plusDays(45),
                "Čeřínek",
                "OOB",
                null,
                coordinator,
                today.plusDays(38),
                List.of("M21", "W21", "M35", "W35", "M50", "W50")
        )));

        eventRepository.save(Event.create(new Event.CreateEvent(
                "Tréninkový závod na Vysočině",
                today.plusDays(60),
                "Žďár nad Sázavou",
                "OOB",
                null,
                null,
                null,
                List.of()
        )));

        eventRepository.save(Event.create(new Event.CreateEvent(
                "Noční sprint v Jihlavě",
                today.plusDays(30),
                "Jihlava centrum",
                "OOB",
                "https://www.oob.cz/zavody/nocni-sprint",
                coordinator,
                today.plusDays(25),
                List.of("M21", "W21", "M40", "W40")
        )));

        eventRepository.save(Event.create(new Event.CreateEvent(
                "Přípravný závod – leso-test",
                today.plusDays(50),
                null,
                "OOB",
                null,
                null,
                null,
                List.of()
        )));

        eventRepository.save(Event.create(new Event.CreateEvent(
                "Moravský pohár – 1. kolo",
                today.plusDays(55),
                "Třebíč",
                "OOB",
                null,
                coordinator,
                today.plusDays(48),
                List.of("M21", "W21", "M35", "W35", "M50", "W50", "M65")
        )));
    }

    private void createActiveEvents(LocalDate today, MemberId coordinator, MemberId regularMember) {
        List<Event> activeEvents = List.of(
                publishedEvent(new Event.CreateEvent(
                        "Šárecký pohár",
                        today.minusDays(50),
                        "Praha – Šárecké údolí",
                        "OOB",
                        "https://oris.orientacnisporty.cz/Zavod?id=50001",
                        coordinator,
                        today.minusDays(55),
                        List.of("M21", "W21")
                )),
                publishedEvent(new Event.CreateEvent(
                        "Jarní pohár Vysočiny",
                        today.minusDays(40),
                        "Čeřínek",
                        "OOB",
                        null,
                        coordinator,
                        today.minusDays(45),
                        List.of("M21", "W21", "M35", "W35")
                )),
                publishedEvent(new Event.CreateEvent(
                        "Mistrovství ČR – sprint",
                        today.minusDays(30),
                        "Brno",
                        "OOB",
                        "https://oris.orientacnisporty.cz/Zavod?id=50002",
                        null,
                        today.minusDays(37),
                        List.of("M21", "W21", "M35", "W35", "M40", "W40")
                )),
                publishedEvent(new Event.CreateEvent(
                        "Černavský sprint",
                        today.minusDays(20),
                        "Černava",
                        "OOB",
                        null,
                        coordinator,
                        today.minusDays(25),
                        List.of()
                )),
                publishedEvent(new Event.CreateEvent(
                        "Ligový závod – Vysočina",
                        today.minusDays(10),
                        null,
                        "OOB",
                        null,
                        null,
                        null,
                        List.of("M21", "W21")
                )),
                publishedEvent(new Event.CreateEvent(
                        "Oblastní závod Jihomoravský kraj",
                        today.minusDays(5),
                        "Brno – Bystrc",
                        "OOB",
                        null,
                        coordinator,
                        today.minusDays(10),
                        List.of("M21", "W21", "M35", "W35", "M50")
                )),
                publishedEvent(new Event.CreateEvent(
                        "Žďárský pohár",
                        today.plusDays(5),
                        "Žďár nad Sázavou",
                        "OOB",
                        "https://oris.orientacnisporty.cz/Zavod?id=50003",
                        null,
                        today.plusDays(2),
                        List.of("M21", "W21", "M35", "W35")
                )),
                publishedEvent(new Event.CreateEvent(
                        "Jihlavský noční sprint",
                        today.plusDays(12),
                        "Jihlava",
                        "OOB",
                        null,
                        coordinator,
                        today.plusDays(8),
                        List.of()
                )),
                publishedEvent(new Event.CreateEvent(
                        "Pohár OOB – 2. kolo",
                        today.plusDays(18),
                        "Třebíč",
                        "OOB",
                        null,
                        null,
                        today.plusDays(14),
                        List.of("M21", "W21", "M40", "W40", "M55")
                )),
                publishedEvent(new Event.CreateEvent(
                        "Juniorský přebor Vysočiny",
                        today.plusDays(22),
                        "Čeřínek",
                        "OOB",
                        null,
                        coordinator,
                        today.plusDays(18),
                        List.of("M18", "W18", "M21", "W21")
                )),
                publishedEvent(new Event.CreateEvent(
                        "Krajský přebor – long",
                        today.plusDays(28),
                        null,
                        "OOB",
                        "https://oris.orientacnisporty.cz/Zavod?id=50004",
                        null,
                        today.plusDays(22),
                        List.of("M21", "W21")
                )),
                publishedEvent(new Event.CreateEvent(
                        "Přebor kraje Vysočina",
                        today.plusDays(35),
                        "Jihlava",
                        "OOB",
                        null,
                        coordinator,
                        today.plusDays(28),
                        List.of("M21", "W21", "M35", "W35", "M50", "W50")
                )),
                publishedEvent(new Event.CreateEvent(
                        "Celorepublikový pohár – 1. kolo",
                        today.plusDays(42),
                        "Šárka",
                        "OOB",
                        "https://oris.orientacnisporty.cz/Zavod?id=50005",
                        null,
                        today.plusDays(35),
                        List.of("M21", "W21", "M35", "W35", "M40", "W40", "M50")
                )),
                publishedEvent(new Event.CreateEvent(
                        "Mistrovství ČR – middle",
                        today.plusDays(48),
                        "Brno – Soběšice",
                        "OOB",
                        null,
                        coordinator,
                        today.plusDays(41),
                        List.of("M21", "W21")
                )),
                publishedEvent(new Event.CreateEvent(
                        "Vysočina open",
                        today.plusDays(55),
                        "Čeřínek",
                        "OOB",
                        "https://oris.orientacnisporty.cz/Zavod?id=50006",
                        null,
                        today.plusDays(48),
                        List.of("M21", "W21", "M35", "W35", "M50", "W50")
                )),
                publishedEvent(new Event.CreateEvent(
                        "Noční závod v Brně",
                        today.plusDays(62),
                        null,
                        "OOB",
                        null,
                        coordinator,
                        today.plusDays(55),
                        List.of()
                )),
                publishedEvent(new Event.CreateEvent(
                        "Závod naděje – kategorie junioři",
                        today.plusDays(10),
                        "Třebíč",
                        "OOB",
                        null,
                        null,
                        today.plusDays(7),
                        List.of("M18", "W18")
                )),
                publishedEvent(new Event.CreateEvent(
                        "Pohár Jihomoravského kraje – kolo 1",
                        today.minusDays(15),
                        "Brno – Komín",
                        "OOB",
                        null,
                        coordinator,
                        today.minusDays(20),
                        List.of("M21", "W21", "M35")
                )),
                publishedEvent(new Event.CreateEvent(
                        "Trénink – terén Černava",
                        today.plusDays(25),
                        "Černava",
                        "OOB",
                        null,
                        null,
                        null,
                        List.of()
                )),
                publishedEvent(new Event.CreateEvent(
                        "Ligový závod – Jihomoravský kraj",
                        today.plusDays(38),
                        "Brno",
                        "OOB",
                        "https://oris.orientacnisporty.cz/Zavod?id=50007",
                        coordinator,
                        today.plusDays(31),
                        List.of("M21", "W21", "M35", "W35", "M40", "W40")
                )),
                publishedEvent(new Event.CreateEvent(
                        "Vysočina cup – závěrečné kolo",
                        today.minusDays(3),
                        null,
                        "OOB",
                        null,
                        null,
                        today.minusDays(7),
                        List.of("M21", "W21")
                )),
                publishedEvent(new Event.CreateEvent(
                        "Ždárský pohár – finále sezóny",
                        today.plusDays(58),
                        "Žďár nad Sázavou",
                        "OOB",
                        "https://oris.orientacnisporty.cz/Zavod?id=50008",
                        coordinator,
                        today.plusDays(50),
                        List.of("M21", "W21", "M35", "W35", "M50", "W50", "M65", "W65")
                ))
        );

        List<Event> savedEvents = new java.util.ArrayList<>();
        for (Event event : activeEvents) {
            savedEvents.add(eventRepository.save(event));
        }

        registerMembers(savedEvents, regularMember, coordinator);
    }

    private void registerMembers(List<Event> savedEvents, MemberId regularMember, MemberId adminMember) {
        LocalDate today = LocalDate.now();

        List<Event> openEvents = savedEvents.stream()
                .filter(e -> e.getEventDate().isAfter(today) && e.areRegistrationsOpen())
                .toList();

        if (regularMember != null) {
            for (int i = 0; i < Math.min(5, openEvents.size()); i++) {
                Event event = openEvents.get(i);
                event.registerMember(regularMember, new SiCardNumber("123456"), resolveFirstCategory(event));
                eventRepository.save(event);
            }
        }

        if (adminMember != null) {
            for (int i = 0; i < Math.min(2, openEvents.size()); i++) {
                Event event = openEvents.get(i);
                if (event.findRegistration(adminMember).isEmpty()) {
                    event.registerMember(adminMember, new SiCardNumber("999001"), resolveFirstCategory(event));
                    eventRepository.save(event);
                }
            }
        }
    }

    private String resolveFirstCategory(Event event) {
        return event.getCategories().isEmpty() ? null : event.getCategories().get(0);
    }

    private static Event publishedEvent(Event.CreateEvent command) {
        Event event = Event.create(command);
        event.publish();
        return event;
    }
}
