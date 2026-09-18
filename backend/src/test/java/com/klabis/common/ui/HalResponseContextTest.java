package com.klabis.common.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HalResponseContext typed context map")
class HalResponseContextTest {

    @BeforeEach
    void bindRequest() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void unbindRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    record EventIdHint(String eventId) {}

    record SyncEnrolled(boolean enrolled) {}

    interface Animal {}

    record Dog(String name) implements Animal {}

    record Cat(String name) implements Animal {}

    @Test
    @DisplayName("getContext returns the stored value for an exact type match")
    void getContextExactMatch() {
        HalResponseContext.setContext(new EventIdHint("E-1"));

        assertThat(HalResponseContext.getContext(EventIdHint.class)).isEqualTo(new EventIdHint("E-1"));
    }

    @Test
    @DisplayName("getContext returns the value when requested through a supertype/interface")
    void getContextViaSupertype() {
        HalResponseContext.setContext(new Dog("Rex"));

        assertThat(HalResponseContext.getContext(Animal.class)).isEqualTo(new Dog("Rex"));
    }

    @Test
    @DisplayName("getContext throws when nothing is stored for the requested type but an unrelated value is stored")
    void getContextThrowsWhenTypeAbsentDespiteUnrelatedValue() {
        HalResponseContext.setContext(new SyncEnrolled(true));

        assertThatThrownBy(() -> HalResponseContext.getContext(EventIdHint.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nothing stored")
                .hasMessageContaining(EventIdHint.class.getName());
    }

    @Test
    @DisplayName("findContext is empty when nothing is stored for the requested type but an unrelated value is stored")
    void findContextEmptyWhenTypeAbsentDespiteUnrelatedValue() {
        HalResponseContext.setContext(new SyncEnrolled(true));

        assertThat(HalResponseContext.findContext(EventIdHint.class)).isEmpty();
    }

    @Test
    @DisplayName("two values of different types coexist, each retrievable")
    void differentTypesCoexist() {
        // Fails against a single-slot implementation: the second setContext would evict the first.
        HalResponseContext.setContext(new EventIdHint("E-2"));
        HalResponseContext.setContext(new SyncEnrolled(true));

        assertThat(HalResponseContext.getContext(EventIdHint.class)).isEqualTo(new EventIdHint("E-2"));
        assertThat(HalResponseContext.getContext(SyncEnrolled.class)).isEqualTo(new SyncEnrolled(true));
    }

    @Test
    @DisplayName("a second setContext of the same type replaces the first")
    void sameTypeReplaces() {
        HalResponseContext.setContext(new EventIdHint("first"));
        HalResponseContext.setContext(new EventIdHint("second"));

        assertThat(HalResponseContext.getContext(EventIdHint.class)).isEqualTo(new EventIdHint("second"));
    }

    @Test
    @DisplayName("ambiguous lookup through a shared supertype throws naming both concrete types")
    void ambiguousLookupThrows() {
        HalResponseContext.setContext(new Dog("Rex"));
        HalResponseContext.setContext(new Cat("Tom"));

        assertThatThrownBy(() -> HalResponseContext.getContext(Animal.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(Dog.class.getName())
                .hasMessageContaining(Cat.class.getName());

        assertThatThrownBy(() -> HalResponseContext.findContext(Animal.class))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("repeated reads both return the value (read does not consume)")
    void repeatedReadsDoNotConsume() {
        // Fails if the read is made consuming, as the take* methods are.
        HalResponseContext.setContext(new EventIdHint("E-3"));

        assertThat(HalResponseContext.getContext(EventIdHint.class)).isEqualTo(new EventIdHint("E-3"));
        assertThat(HalResponseContext.getContext(EventIdHint.class)).isEqualTo(new EventIdHint("E-3"));
    }

    @Test
    @DisplayName("no request attributes bound: findContext empty, getContext throws, setContext is a no-op")
    void noRequestBound() {
        RequestContextHolder.resetRequestAttributes();

        HalResponseContext.setContext(new EventIdHint("ignored"));

        assertThat(HalResponseContext.findContext(EventIdHint.class)).isEmpty();
        assertThatThrownBy(() -> HalResponseContext.getContext(EventIdHint.class))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("after clear() a subsequent findContext finds nothing")
    void clearRemovesContextMap() {
        HalResponseContext.setContext(new EventIdHint("E-4"));

        HalResponseContext.clear();

        assertThat(HalResponseContext.findContext(EventIdHint.class)).isEmpty();
    }
}
