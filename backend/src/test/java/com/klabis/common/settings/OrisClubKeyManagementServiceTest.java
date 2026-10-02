package com.klabis.common.settings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OrisClubKeyManagementService")
class OrisClubKeyManagementServiceTest {

    private final OrisClubKeyPort orisClubKeyPort = mock(OrisClubKeyPort.class);
    private final OrisClubKeyManagementPort service = new OrisClubKeyManagementService(orisClubKeyPort);

    @Test
    @DisplayName("store delegates to the secondary port")
    void storeDelegates() {
        service.store("secret");

        verify(orisClubKeyPort).store("secret");
    }

    @Test
    @DisplayName("clear delegates to the secondary port")
    void clearDelegates() {
        service.clear();

        verify(orisClubKeyPort).clear();
    }

    @Test
    @DisplayName("isSet reports the state of the secondary port")
    void isSetDelegates() {
        when(orisClubKeyPort.isSet()).thenReturn(true);

        assertThat(service.isSet()).isTrue();
    }

    @Test
    @DisplayName("exposes no way to read the key back")
    void hasNoGetter() {
        for (Method method : OrisClubKeyManagementPort.class.getDeclaredMethods()) {
            assertThat(method.getReturnType() == String.class).isFalse();
        }
    }
}
