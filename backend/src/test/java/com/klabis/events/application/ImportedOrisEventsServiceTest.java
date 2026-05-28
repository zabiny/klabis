package com.klabis.events.application;

import com.klabis.events.domain.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImportedOrisEventsService")
class ImportedOrisEventsServiceTest {

    @Mock
    private EventRepository eventRepository;

    private ImportedOrisEventsService service;

    @BeforeEach
    void setUp() {
        service = new ImportedOrisEventsService(eventRepository);
    }

    @Test
    @DisplayName("should delegate to EventRepository and return imported IDs from candidates")
    void shouldDelegateToRepositoryAndReturnImportedIds() {
        List<Integer> candidates = List.of(101, 102, 999);
        when(eventRepository.findImportedOrisIds(candidates)).thenReturn(Set.of(101, 102));

        Set<Integer> result = service.findImportedOrisIds(candidates);

        assertThat(result).containsExactlyInAnyOrder(101, 102);
        verify(eventRepository).findImportedOrisIds(candidates);
    }

    @Test
    @DisplayName("should return empty set when no candidates are imported")
    void shouldReturnEmptySetWhenNoCandidatesImported() {
        List<Integer> candidates = List.of(777, 888);
        when(eventRepository.findImportedOrisIds(candidates)).thenReturn(Set.of());

        Set<Integer> result = service.findImportedOrisIds(candidates);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty set for empty candidate collection")
    void shouldReturnEmptySetForEmptyCandidates() {
        when(eventRepository.findImportedOrisIds(List.of())).thenReturn(Set.of());

        Set<Integer> result = service.findImportedOrisIds(List.of());

        assertThat(result).isEmpty();
        verify(eventRepository).findImportedOrisIds(List.of());
    }
}
