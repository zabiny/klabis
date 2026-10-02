package com.klabis.sync.application;

import com.klabis.sync.domain.SyncProjection;
import com.klabis.sync.domain.SyncProjectionFieldReader;
import com.klabis.sync.fixtures.TestSyncProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SyncProjectionFieldsService")
class SyncProjectionFieldsServiceTest {

    private final SyncProjectionFieldReader reader = mock(SyncProjectionFieldReader.class);
    private final SyncProjectionFieldsPort port = new SyncProjectionFieldsService(reader);

    @Test
    @DisplayName("delegates to the domain field reader")
    void delegatesToReader() {
        SyncProjection projection = new TestSyncProjection("Sprint", "Brno");
        when(reader.fields(projection)).thenReturn(Map.of("name", "Sprint"));

        assertThat(port.fields(projection)).isEqualTo(Map.of("name", "Sprint"));
    }

    @Test
    @DisplayName("rejects null projection")
    void rejectsNull() {
        assertThatThrownBy(() -> port.fields(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
