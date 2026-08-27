package com.klabis.events.infrastructure.sync;

import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.klabis.events.WebsiteUrl;
import org.springframework.core.convert.converter.Converter;

/**
 * The ORIS event {@code SyncLine} reverse converter: turns the raw {@link OrisEventSyncData}
 * (external payload) into the domain-shaped {@link EventSyncData} (local payload). This is where the
 * {@code com.dpolach.api.orisclient} DTO translation lives — organizer fallback, deadline validation,
 * category extraction, ranking, base entry fee, ORIS-discipline to event-type lookup — so nothing
 * downstream of the {@code SyncLine} depends on the ORIS client library.
 * <p>
 * Not a Spring bean: {@link EventSyncConfiguration} instantiates it by hand. Registering it as a
 * {@code Converter} bean would pull it into {@code mvcConversionService}, where {@code @WebMvcTest}
 * slices scan it without its MapStruct collaborator and fail to start.
 */
class EventSyncDataConverter implements Converter<OrisEventSyncData, EventSyncData> {

    private final OrisEventDetailsMapper detailsMapper;
    private final OrisEventMappingSupport support;
    private final OrisWebUrls orisWebUrls;

    EventSyncDataConverter(OrisEventDetailsMapper detailsMapper,
                           OrisEventMappingSupport support,
                           OrisWebUrls orisWebUrls) {
        this.detailsMapper = detailsMapper;
        this.support = support;
        this.orisWebUrls = orisWebUrls;
    }

    @Override
    public EventSyncData convert(OrisEventSyncData source) {
        int orisId = source.orisId();
        EventDetails details = source.details();
        OrisEventDetailsMapper.TrivialEventFields trivial = detailsMapper.toTrivialFields(details);
        return new EventSyncData(
                null,
                orisId,
                trivial.name(),
                trivial.eventDate(),
                trivial.location(),
                support.resolveOrganizer(details),
                WebsiteUrl.of(orisWebUrls.eventUrl(orisId)),
                support.buildRegistrationDeadlines(details, orisId),
                support.extractCategories(details),
                support.resolveRanking(details.level()),
                support.deriveBaseEntryFee(details),
                support.resolveEventType(details.discipline()));
    }
}
