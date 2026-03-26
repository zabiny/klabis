package com.klabis.common.patch;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PatchField Jackson Deserializer Tests")
class PatchFieldDeserializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new JsonMapper();
        PatchFieldModule module = new PatchFieldModule();
        objectMapper.registerModule(module);
    }

    @Nested
    @DisplayName("Deserialization of provided values")
    class ProvidedValuesTests {

        @Test
        @DisplayName("Should deserialize present string field as provided")
        void shouldDeserializePresentStringFieldAsProvided() throws JacksonException {
            String json = """
                    {
                        "name": "John Doe"
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.name().isProvided()).isTrue();
            assertThat(dto.name().throwIfNotProvided()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should deserialize present integer field as provided")
        void shouldDeserializePresentIntegerFieldAsProvided() throws JacksonException {
            String json = """
                    {
                        "age": 25
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.age().isProvided()).isTrue();
            assertThat(dto.age().throwIfNotProvided()).isEqualTo(25);
        }

        @Test
        @DisplayName("Should deserialize present boolean field as provided")
        void shouldDeserializePresentBooleanFieldAsProvided() throws JacksonException {
            String json = """
                    {
                        "active": true
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.active().isProvided()).isTrue();
            assertThat(dto.active().throwIfNotProvided()).isTrue();
        }
    }

    @Nested
    @DisplayName("Deserialization of absent fields")
    class AbsentFieldsTests {

        @Test
        @DisplayName("Should deserialize absent string field as not provided")
        void shouldDeserializeAbsentStringFieldAsNotProvided() throws JacksonException {
            String json = """
                    {
                        "age": 25
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.name().isProvided()).isFalse();
        }

        @Test
        @DisplayName("Should deserialize absent integer field as not provided")
        void shouldDeserializeAbsentIntegerFieldAsNotProvided() throws JacksonException {
            String json = """
                    {
                        "name": "John"
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.age().isProvided()).isFalse();
        }

        @Test
        @DisplayName("Should deserialize absent boolean field as not provided")
        void shouldDeserializeAbsentBooleanFieldAsNotProvided() throws JacksonException {
            String json = """
                    {
                        "name": "John"
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.active().isProvided()).isFalse();
        }

        @Test
        @DisplayName("Should deserialize all absent fields as not provided")
        void shouldDeserializeAllAbsentFieldsAsNotProvided() throws JacksonException {
            String json = """
                    {
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.name().isProvided()).isFalse();
            assertThat(dto.age().isProvided()).isFalse();
            assertThat(dto.active().isProvided()).isFalse();
        }
    }

    @Nested
    @DisplayName("Deserialization of explicit null values")
    class NullValuesTests {

        @Test
        @DisplayName("Should deserialize explicit null string as provided with null")
        void shouldDeserializeExplicitNullStringAsProvidedWithNull() throws JacksonException {
            String json = """
                    {
                        "name": null
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.name().isProvided()).isTrue();
            assertThat(dto.name().throwIfNotProvided()).isNull();
        }

        @Test
        @DisplayName("Should deserialize explicit null integer as provided with null")
        void shouldDeserializeExplicitNullIntegerAsProvidedWithNull() throws JacksonException {
            String json = """
                    {
                        "age": null
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.age().isProvided()).isTrue();
            assertThat(dto.age().throwIfNotProvided()).isNull();
        }

        @Test
        @DisplayName("Should deserialize explicit null boolean as provided with null")
        void shouldDeserializeExplicitNullBooleanAsProvidedWithNull() throws JacksonException {
            String json = """
                    {
                        "active": null
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.active().isProvided()).isTrue();
            assertThat(dto.active().throwIfNotProvided()).isNull();
        }
    }

    @Nested
    @DisplayName("Deserialization of mixed scenarios")
    class MixedScenariosTests {

        @Test
        @DisplayName("Should deserialize mix of provided, absent, and null fields")
        void shouldDeserializeMixOfProvidedAbsentAndNullFields() throws JacksonException {
            String json = """
                    {
                        "name": "John",
                        "age": null,
                        "active": true
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.name().isProvided()).isTrue();
            assertThat(dto.name().throwIfNotProvided()).isEqualTo("John");

            assertThat(dto.age().isProvided()).isTrue();
            assertThat(dto.age().throwIfNotProvided()).isNull();

            assertThat(dto.active().isProvided()).isTrue();
            assertThat(dto.active().throwIfNotProvided()).isTrue();
        }

        @Test
        @DisplayName("Should deserialize empty string as provided")
        void shouldDeserializeEmptyStringAsProvided() throws JacksonException {
            String json = """
                    {
                        "name": ""
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.name().isProvided()).isTrue();
            assertThat(dto.name().throwIfNotProvided()).isEmpty();
        }

        @Test
        @DisplayName("Should deserialize zero as provided")
        void shouldDeserializeZeroAsProvided() throws JacksonException {
            String json = """
                    {
                        "age": 0
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.age().isProvided()).isTrue();
            assertThat(dto.age().throwIfNotProvided()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should deserialize false as provided")
        void shouldDeserializeFalseAsProvided() throws JacksonException {
            String json = """
                    {
                        "active": false
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.active().isProvided()).isTrue();
            assertThat(dto.active().throwIfNotProvided()).isFalse();
        }
    }

    record TestDto(
            PatchField<String> name,
            PatchField<Integer> age,
            PatchField<Boolean> active
    ) {
    }
}
