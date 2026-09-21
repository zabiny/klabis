package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.dto.lov.DisciplineListEntry;
import com.klabis.events.domain.Discipline;
import com.klabis.sync.infrastructure.SyncProjectionCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("DisciplineProjectionMapper")
class DisciplineProjectionMapperTest {

    @Nested
    @DisplayName("fromDiscipline()")
    class FromDisciplineMethod {

        @Test
        @DisplayName("maps the local Discipline's code and name into the projection")
        void mapsCodeAndName() {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));

            DisciplineProjection projection = DisciplineProjectionMapper.fromDiscipline(discipline);

            assertThat(projection.code()).isEqualTo("OB");
            assertThat(projection.name()).isEqualTo("Orientační běh");
        }
    }

    @Nested
    @DisplayName("fromOrisEntry()")
    class FromOrisEntryMethod {

        @Test
        @DisplayName("maps name to code and descriptionCZ to name")
        void mapsNameAndDescription() {
            DisciplineListEntry entry = Mockito.mock(DisciplineListEntry.class);
            when(entry.name()).thenReturn("OB");
            when(entry.descriptionCZ()).thenReturn("Orientační běh");

            DisciplineProjection projection = DisciplineProjectionMapper.fromOrisEntry(entry);

            assertThat(projection.code()).isEqualTo("OB");
            assertThat(projection.name()).isEqualTo("Orientační běh");
        }

        @Test
        @DisplayName("falls back to name when ORIS sends no descriptionCZ, mirroring OrisRankingOptionsAdapter's rule")
        void fallsBackToNameWhenDescriptionMissing() {
            DisciplineListEntry entry = Mockito.mock(DisciplineListEntry.class);
            when(entry.name()).thenReturn("OB");
            when(entry.descriptionCZ()).thenReturn(null);

            DisciplineProjection projection = DisciplineProjectionMapper.fromOrisEntry(entry);

            assertThat(projection.code()).isEqualTo("OB");
            assertThat(projection.name()).isEqualTo("OB");
        }

        @Test
        @DisplayName("falls back to name when ORIS sends a blank descriptionCZ")
        void fallsBackToNameWhenDescriptionBlank() {
            DisciplineListEntry entry = Mockito.mock(DisciplineListEntry.class);
            when(entry.name()).thenReturn("OB");
            when(entry.descriptionCZ()).thenReturn("   ");

            DisciplineProjection projection = DisciplineProjectionMapper.fromOrisEntry(entry);

            assertThat(projection.name()).isEqualTo("OB");
        }
    }

    @Test
    @DisplayName("mapping the local side and the ORIS side of equal data hashes equally")
    void fromDiscipline_andFromOrisEntry_withEqualData_hashEqually() {
        Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));

        DisciplineListEntry entry = Mockito.mock(DisciplineListEntry.class);
        when(entry.name()).thenReturn("OB");
        when(entry.descriptionCZ()).thenReturn("Orientační běh");

        DisciplineProjection fromLocal = DisciplineProjectionMapper.fromDiscipline(discipline);
        DisciplineProjection fromOris = DisciplineProjectionMapper.fromOrisEntry(entry);

        assertThat(SyncProjectionCodec.hash(fromLocal)).isEqualTo(SyncProjectionCodec.hash(fromOris));
    }

    @Test
    @DisplayName("a changed field on either side hashes differently")
    void fromDiscipline_andFromOrisEntry_withDifferentData_hashDifferently() {
        Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));

        DisciplineListEntry entry = Mockito.mock(DisciplineListEntry.class);
        when(entry.name()).thenReturn("SP");
        when(entry.descriptionCZ()).thenReturn("Sprint");

        DisciplineProjection fromLocal = DisciplineProjectionMapper.fromDiscipline(discipline);
        DisciplineProjection fromOris = DisciplineProjectionMapper.fromOrisEntry(entry);

        assertThat(SyncProjectionCodec.hash(fromLocal)).isNotEqualTo(SyncProjectionCodec.hash(fromOris));
    }
}
