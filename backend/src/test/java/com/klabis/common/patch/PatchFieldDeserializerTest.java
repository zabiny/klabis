package com.klabis.common.patch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PatchField Jackson Deserializer Tests")
class PatchFieldDeserializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        PatchFieldModule module = new PatchFieldModule();
        objectMapper.registerModule(module);
    }

    @Nested
    @DisplayName("Deserialization of provided values")
    class ProvidedValuesTests {

        @Test
        @DisplayName("Should deserialize present string field as provided")
        void shouldDeserializePresentStringFieldAsProvided() throws JsonProcessingException {
            String json = """
                    {
                        "name": "John Doe"
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.name().isProvided()).isTrue();
            assertThat(dto.name().get()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should deserialize present integer field as provided")
        void shouldDeserializePresentIntegerFieldAsProvided() throws JsonProcessingException {
            String json = """
                    {
                        "age": 25
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.age().isProvided()).isTrue();
            assertThat(dto.age().get()).isEqualTo(25);
        }

        @Test
        @DisplayName("Should deserialize present boolean field as provided")
        void shouldDeserializePresentBooleanFieldAsProvided() throws JsonProcessingException {
            String json = """
                    {
                        "active": true
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.active().isProvided()).isTrue();
            assertThat(dto.active().get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Deserialization of absent fields")
    class AbsentFieldsTests {

        @Test
        @DisplayName("Should deserialize absent string field as not provided")
        void shouldDeserializeAbsentStringFieldAsNotProvided() throws JsonProcessingException {
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
        void shouldDeserializeAbsentIntegerFieldAsNotProvided() throws JsonProcessingException {
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
        void shouldDeserializeAbsentBooleanFieldAsNotProvided() throws JsonProcessingException {
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
        void shouldDeserializeAllAbsentFieldsAsNotProvided() throws JsonProcessingException {
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
        void shouldDeserializeExplicitNullStringAsProvidedWithNull() throws JsonProcessingException {
            String json = """
                    {
                        "name": null
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.name().isProvided()).isTrue();
            assertThat(dto.name().get()).isNull();
        }

        @Test
        @DisplayName("Should deserialize explicit null integer as provided with null")
        void shouldDeserializeExplicitNullIntegerAsProvidedWithNull() throws JsonProcessingException {
            String json = """
                    {
                        "age": null
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.age().isProvided()).isTrue();
            assertThat(dto.age().get()).isNull();
        }

        @Test
        @DisplayName("Should deserialize explicit null boolean as provided with null")
        void shouldDeserializeExplicitNullBooleanAsProvidedWithNull() throws JsonProcessingException {
            String json = """
                    {
                        "active": null
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.active().isProvided()).isTrue();
            assertThat(dto.active().get()).isNull();
        }
    }

    @Nested
    @DisplayName("Deserialization of mixed scenarios")
    class MixedScenariosTests {

        @Test
        @DisplayName("Should deserialize mix of provided, absent, and null fields")
        void shouldDeserializeMixOfProvidedAbsentAndNullFields() throws JsonProcessingException {
            String json = """
                    {
                        "name": "John",
                        "age": null,
                        "active": true
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.name().isProvided()).isTrue();
            assertThat(dto.name().get()).isEqualTo("John");

            assertThat(dto.age().isProvided()).isTrue();
            assertThat(dto.age().get()).isNull();

            assertThat(dto.active().isProvided()).isTrue();
            assertThat(dto.active().get()).isTrue();
        }

        @Test
        @DisplayName("Should deserialize empty string as provided")
        void shouldDeserializeEmptyStringAsProvided() throws JsonProcessingException {
            String json = """
                    {
                        "name": ""
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.name().isProvided()).isTrue();
            assertThat(dto.name().get()).isEmpty();
        }

        @Test
        @DisplayName("Should deserialize zero as provided")
        void shouldDeserializeZeroAsProvided() throws JsonProcessingException {
            String json = """
                    {
                        "age": 0
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.age().isProvided()).isTrue();
            assertThat(dto.age().get()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should deserialize false as provided")
        void shouldDeserializeFalseAsProvided() throws JsonProcessingException {
            String json = """
                    {
                        "active": false
                    }
                    """;

            TestDto dto = objectMapper.readValue(json, TestDto.class);

            assertThat(dto.active().isProvided()).isTrue();
            assertThat(dto.active().get()).isFalse();
        }
    }

    record TestDto(
            PatchField<String> name,
            PatchField<Integer> age,
            PatchField<Boolean> active
    ) {
    }
}
