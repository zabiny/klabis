package club.klabis.events.oris;

import club.klabis.events.domain.OrisEventId;
import club.klabis.events.oris.dto.OrisData;
import club.klabis.events.oris.dto.OrisEventListFilter;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Optional;
import java.util.stream.Stream;

@SecondaryPort
public interface OrisEventDataSource {
    Optional<OrisData> getOrisEventData(OrisEventId orisEventId);

    Stream<OrisData> streamOrisEvents(OrisEventListFilter filter);
}
