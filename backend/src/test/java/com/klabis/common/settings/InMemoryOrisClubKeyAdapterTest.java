package com.klabis.common.settings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@DisplayName("InMemoryOrisClubKeyAdapter")
class InMemoryOrisClubKeyAdapterTest {

    private final OrisClubKeyPort port = new InMemoryOrisClubKeyAdapter();

    @Test
    @DisplayName("isSet() is false before any key is stored")
    void isSetFalseInitially() {
        assertThat(port.isSet()).isFalse();
    }

    @Test
    @DisplayName("isSet() is true after store(...)")
    void isSetTrueAfterStore() {
        port.store("some-club-key");

        assertThat(port.isSet()).isTrue();
    }

    @Test
    @DisplayName("isSet() is false again after clear()")
    void isSetFalseAfterClear() {
        port.store("some-club-key");

        port.clear();

        assertThat(port.isSet()).isFalse();
    }

    @Test
    @DisplayName("store(\"\") is refused and leaves a previously held key in force")
    void storeEmptyStringIsRefused() {
        port.store("original-key");

        assertThatIllegalArgumentException().isThrownBy(() -> port.store(""));

        assertThat(port.isSet()).isTrue();
    }

    @Test
    @DisplayName("store(\"   \") is refused and leaves a previously held key in force")
    void storeBlankStringIsRefused() {
        port.store("original-key");

        assertThatIllegalArgumentException().isThrownBy(() -> port.store("   "));

        assertThat(port.isSet()).isTrue();
    }

    @Test
    @DisplayName("the port interface exposes no way to read the stored value back")
    void portExposesNoGetter() {
        // compile-level guarantee (design.md D9): only store/isSet/clear may exist,
        // so there is no method whose declared return type could ever carry the key out
        Method[] methods = OrisClubKeyPort.class.getDeclaredMethods();

        assertThat(methods).extracting(Method::getName)
                .containsExactlyInAnyOrder("store", "isSet", "clear");
        assertThat(methods).allSatisfy(method -> assertThat(method.getReturnType())
                .as("method %s must not return a value that could disclose the stored key", method.getName())
                .isIn(void.class, boolean.class));
    }
}
