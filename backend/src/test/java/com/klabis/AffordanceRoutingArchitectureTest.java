package com.klabis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the invariant that HAL affordances and links resolve against the generated {@code *Api}
 * interfaces rather than the concrete controllers.
 *
 * <p>Java does not inherit parameter annotations from an interface, so when {@code methodOn(...)}
 * records a controller method, {@code HalFormsSupport.modifyAffordanceForHalForms} finds no
 * {@code @RequestBody} unless the override happens to repeat it. It then returns the affordance
 * unmodified, skipping {@code HalFormsInputPayloadMetadata} — the code that supplies inline options,
 * {@code @HalForms(access = …)} and, most visibly, the {@code readOnly} flag. Three modules shipped
 * templates whose every field was {@code readOnly: true} because of exactly that.
 *
 * <p>The failure is silent: no exception, no failing link assertion, just a form the UI cannot
 * render. A structural check is therefore the only reliable guard — a behavioural one would have to
 * enumerate every affordance to notice the absence.
 *
 * <p>This inspects sources rather than bytecode because the target type is an argument to
 * {@code methodOn(...)}; ArchUnit sees the call but not which class literal was passed.
 */
@DisplayName("Affordance routing architecture")
class AffordanceRoutingArchitectureTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java/com/klabis");

    private static final Pattern METHOD_ON_CONTROLLER =
            Pattern.compile("methodOn\\(\\s*(\\w*Controller)\\.class\\s*\\)");

    @Test
    @DisplayName("no production call site records a controller method via methodOn")
    void methodOnShouldTargetGeneratedInterfaces() throws IOException {
        List<String> offenders;
        try (Stream<Path> sources = Files.walk(MAIN_SOURCES)) {
            offenders = sources.filter(path -> path.toString().endsWith(".java"))
                    .flatMap(AffordanceRoutingArchitectureTest::offendingCallSites)
                    .toList();
        }

        assertThat(offenders)
                .as("methodOn(...) must record the generated *Api interface, not the controller — "
                    + "otherwise HAL-FORMS input metadata is silently dropped from the affordance")
                .isEmpty();
    }

    private static Stream<String> offendingCallSites(Path source) {
        List<String> lines;
        try {
            lines = Files.readAllLines(source, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + source, e);
        }

        return java.util.stream.IntStream.range(0, lines.size())
                .mapToObj(index -> {
                    Matcher matcher = METHOD_ON_CONTROLLER.matcher(lines.get(index));
                    return matcher.find()
                            ? "%s:%d references %s".formatted(source, index + 1, matcher.group(1))
                            : null;
                })
                .filter(java.util.Objects::nonNull);
    }
}
