package com.klabis.common.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    @DisplayName("deferred context supplier is not invoked when never read")
    void deferredNotInvokedWhenUnread() {
        AtomicInteger calls = new AtomicInteger();
        HalResponseContext.setDeferredContext(EventIdHint.class, () -> {
            calls.incrementAndGet();
            return new EventIdHint("lazy");
        });

        HalResponseContext.clear();

        assertThat(calls).hasValue(0);
    }

    @Test
    @DisplayName("deferred context is resolved on first read and memoized")
    void deferredResolvedOnceAndMemoized() {
        AtomicInteger calls = new AtomicInteger();
        HalResponseContext.setDeferredContext(EventIdHint.class, () -> {
            calls.incrementAndGet();
            return new EventIdHint("lazy");
        });

        assertThat(calls).hasValue(0);
        assertThat(HalResponseContext.getContext(EventIdHint.class)).isEqualTo(new EventIdHint("lazy"));
        assertThat(HalResponseContext.findContext(EventIdHint.class)).contains(new EventIdHint("lazy"));
        assertThat(calls).hasValue(1);
    }

    @Test
    @DisplayName("deferred context resolves through a supertype and coexists with eager values")
    void deferredViaSupertypeAndCoexists() {
        HalResponseContext.setContext(new SyncEnrolled(true));
        HalResponseContext.setDeferredContext(Dog.class, () -> new Dog("Rex"));

        assertThat(HalResponseContext.getContext(Animal.class)).isEqualTo(new Dog("Rex"));
        assertThat(HalResponseContext.getContext(SyncEnrolled.class)).isEqualTo(new SyncEnrolled(true));
    }

    @Test
    @DisplayName("ambiguity between deferred and eager values is detected without invoking the supplier")
    void deferredAmbiguityDoesNotInvokeSupplier() {
        AtomicInteger calls = new AtomicInteger();
        HalResponseContext.setContext(new Dog("Rex"));
        HalResponseContext.setDeferredContext(Cat.class, () -> {
            calls.incrementAndGet();
            return new Cat("Tom");
        });

        assertThatThrownBy(() -> HalResponseContext.getContext(Animal.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(Dog.class.getName())
                .hasMessageContaining(Cat.class.getName());
        assertThat(calls).hasValue(0);
    }

    @Test
    @DisplayName("a null deferred result is memoized and reads as absent")
    void deferredNullMemoized() {
        AtomicInteger calls = new AtomicInteger();
        HalResponseContext.setDeferredContext(EventIdHint.class, () -> {
            calls.incrementAndGet();
            return null;
        });

        assertThat(HalResponseContext.findContext(EventIdHint.class)).isEmpty();
        assertThat(HalResponseContext.findContext(EventIdHint.class)).isEmpty();
        assertThat(calls).hasValue(1);
    }

    @Test
    @DisplayName("embedDeferred is not invoked until taken, and then invoked once")
    void embedDeferredLazy() {
        AtomicInteger calls = new AtomicInteger();
        HalResponseContext.embedDeferred(() -> {
            calls.incrementAndGet();
            return List.of("a", "b");
        }, String.class);

        assertThat(calls).hasValue(0);
        HalResponseContext.Embedded embedded = HalResponseContext.takeEmbedded();

        assertThat(calls).hasValue(1);
        assertThat((java.util.Collection<Object>) embedded.collection()).containsExactly("a", "b");
        assertThat(embedded.itemType()).isEqualTo(String.class);
        assertThat(HalResponseContext.takeEmbedded()).isNull();
        assertThat(calls).hasValue(1);
    }

    @Test
    @DisplayName("clear() discards unresolved deferred embedded and context suppliers without invoking them")
    void clearDiscardsDeferred() {
        AtomicInteger calls = new AtomicInteger();
        HalResponseContext.embedDeferred(() -> {
            calls.incrementAndGet();
            return List.of();
        }, String.class);
        HalResponseContext.setDeferredContext(EventIdHint.class, () -> {
            calls.incrementAndGet();
            return new EventIdHint("x");
        });

        HalResponseContext.clear();

        assertThat(HalResponseContext.takeEmbedded()).isNull();
        assertThat(HalResponseContext.findContext(EventIdHint.class)).isEmpty();
        assertThat(calls).hasValue(0);
    }

    @Test
    @DisplayName("markHalResource is taken once and discarded by clear()")
    void markHalResource() {
        assertThat(HalResponseContext.takeHalResourceMark()).isFalse();

        HalResponseContext.markHalResource();
        assertThat(HalResponseContext.takeHalResourceMark()).isTrue();
        assertThat(HalResponseContext.takeHalResourceMark()).isFalse();

        HalResponseContext.markHalResource();
        HalResponseContext.clear();
        assertThat(HalResponseContext.takeHalResourceMark()).isFalse();
    }
}
