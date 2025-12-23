package club.klabis.oris.infrastructure.apiclient;

import club.klabis.oris.application.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RestClientTest(OrisApiClientConfiguration.class)
class OrisApiClientTest {

    @Autowired
    OrisApiClient testedClient;

    @Autowired
    MockRestServiceServer restServiceServer;

    private DefaultResponseCreator withJsonResponse(int status, String body) {
        return withStatus(HttpStatusCode.valueOf(status)).body(body)
                .contentType(MediaType.parseMediaType("application/javascript"));
    }

    private DefaultResponseCreator withJsonResponseHavingBodyFromResourceFile(int status, String resourceFilePath) throws IOException {
        final String jsonBody = new ClassPathResource(resourceFilePath).getContentAsString(StandardCharsets.UTF_8);
        return withJsonResponse(status, jsonBody);
    }


    @DisplayName("getUser API tests")
    @Nested
    class GetUserApiTests {
        @Test
        @DisplayName("it returns expected response when member is not found")
        void checkMemberNotFoundHandling() throws IOException {
            // ORIS returns HTTP 200 with [] in Data when member is not found.
            // That means it's wrong type (causes parsing error).
            // This test makes sure that "workaround" what is in place for that case in configured API client works properly and throws NotFound exception instead of behaving like successs and returning bad data
            restServiceServer.expect(MockRestRequestMatchers.anything())
                    .andRespond(withJsonResponseHavingBodyFromResourceFile(200, "oris/getUserNotFoundResponse.json"));

            assertThatThrownBy(() -> testedClient.getUserInfo("32323"))
                    .isInstanceOf(HttpClientErrorException.NotFound.class);
        }

        @Test
        @DisplayName("it should return parsed data from response")
        void itShouldReturnExpectedData() throws IOException {
            restServiceServer.expect(MockRestRequestMatchers.anything())
                    .andRespond(withJsonResponseHavingBodyFromResourceFile(200, "oris/getUserExampleResponse.json"));

            OrisApiClient.OrisResponse<OrisUserInfo> actualResponse = testedClient.getUserInfo("32323");

            assertThat(actualResponse.data()).extracting("orisId", "firstName", "lastName")
                    .containsExactly(452, "John", "Doe");
            assertThat(actualResponse.status()).isEqualTo("OK");
            assertThat(actualResponse.format()).isEqualTo("json");
            assertThat(actualResponse.method()).isEqualTo("getUser");
        }

        @Test
        @DisplayName("it should call expected API with parameters")
        void itShouldCallExpectedApi() throws IOException {
            restServiceServer.expect(MockRestRequestMatchers.requestTo(
                            "https://oris.orientacnisporty.cz/API/?format=json&method=getUser&rgnum=32323"))
                    .andRespond(withJsonResponseHavingBodyFromResourceFile(200, "oris/getUserExampleResponse.json"));

            testedClient.getUserInfo("32323");

            restServiceServer.verify();
        }
    }

    @Nested
    @DisplayName("getEventList API tests")
    class GetEventListApiTests {
        @Test
        @DisplayName("it should return parsed data from response")
        void itShouldReturnExpectedData() throws IOException {
            restServiceServer.expect(MockRestRequestMatchers.anything())
                    .andRespond(withJsonResponseHavingBodyFromResourceFile(200, "oris/getEventListResponse.json"));

            OrisApiClient.OrisResponse<Map<String, EventSummary>> expectedData = new OrisApiClient.OrisResponse<>(
                    Map.of("Event_9160", new EventSummary(
                            9160,
                            "Západočeský žebříček - jaro 2025",
                            LocalDate.of(2025, 1, 1),
                            "",
                            new Organizer(
                                    646, "ZCO", "Západočeská oblast"
                            ),
                            new Level(
                                    4, "OŽ", "Oblastní žebříček", "Local event"
                            ),
                            new Sport(
                                    1, "OB", "Foot O"
                            ),
                            new Discipline(
                                    10, "Z", "Dlouhodobé žebříčky", "Cups and ranking"
                            ),
                            LocalDateTime.parse("2025-01-05T23:59:59"),
                            LocalDateTime.parse("2025-01-07T23:59:59"),
                            null)
                    ),
                    "json",
                    "OK",
                    LocalDateTime.of(2025, 03, 15, 1, 28, 59),
                    "getEventList"
            );

            var result = testedClient.getEventList(null);

            assertThat(result).usingRecursiveComparison().isEqualTo(expectedData);
        }

        @Test
        @DisplayName("it should call expected API without defined parameters")
        void itShouldCallExpectedApiNoParams() throws IOException {
            restServiceServer.expect(MockRestRequestMatchers.requestTo(
                            "https://oris.orientacnisporty.cz/API/?format=json&method=getEventList&myClubId=205"))
                    .andRespond(withJsonResponseHavingBodyFromResourceFile(200, "oris/getEventListResponse.json"));

            testedClient.getEventList(null);

            restServiceServer.verify();
        }

        @Test
        @DisplayName("it should call expected API with defined parameters")
        void itShouldCallExpectedApiWithParams() throws IOException {
            restServiceServer.expect(MockRestRequestMatchers.requestTo(
                            "https://oris.orientacnisporty.cz/API/?format=json&method=getEventList&myClubId=205&datefrom=2020-10-01&dateto=2023-04-10&rg=JM"))
                    .andRespond(withJsonResponseHavingBodyFromResourceFile(200, "oris/getEventListResponse.json"));

            testedClient.getEventList(OrisEventListFilter.EMPTY.withRegion(OrisApiClient.REGION_JIHOMORAVSKA)
                    .withDateFrom(LocalDate.of(2020, 10, 1))
                    .withDateTo(LocalDate.of(2023, 4, 10)));

            restServiceServer.verify();
        }

    }


    @DisplayName("getEventDetails API tests")
    @Nested
    class GetEventDetailsApiTests {
        @Test
        @DisplayName("it should return parsed data from response")
        void itShouldReturnExpectedData() throws IOException {
            restServiceServer.expect(MockRestRequestMatchers.anything())
                    .andRespond(withJsonResponseHavingBodyFromResourceFile(200, "oris/getEventDetailsResponse.json"));

            var actualResponse = testedClient.getEventDetails(12345);

            EventDetails expected = EventDetailsBuilder.builder()
                    .id(2252)
                    .name("Haná Orienteering Festival")
                    .date(LocalDate.of(2013, 3, 30))
                    .currentEntriesCount(263)
                    .entryDate1(ZonedDateTime.parse("2013-03-05T23:59:59+01:00[Europe/Prague]"))
                    .entryDate2(ZonedDateTime.parse("2013-03-23T21:30:59+01:00[Europe/Prague]"))
                    .entryDate3(null)
                    .build();

            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.status()).isEqualTo("OK");
            assertThat(actualResponse.format()).isEqualTo("json");
            assertThat(actualResponse.method()).isEqualTo("getEvent");
            assertThat(actualResponse.data()).isNotNull();
            assertThat(actualResponse.data())
                    .usingRecursiveComparison()
                    .ignoringExpectedNullFields()
                    .isEqualTo(expected);

            // some minimal check that classes were parsed
            assertThat(actualResponse.data().classes())
                    .values().extracting(EventClass::name)
                    .containsExactly("D10",
                            "D12",
                            "D14",
                            "D16",
                            "D18",
                            "D21A",
                            "D21B",
                            "D35",
                            "D45",
                            "D55",
                            "DH10N",
                            "H10",
                            "H12",
                            "H14",
                            "H16",
                            "H18",
                            "H21A",
                            "H21B",
                            "H35",
                            "H45",
                            "H55",
                            "H65",
                            "HDR",
                            "P");
        }

        @Test
        @DisplayName("it should call expected API with parameters")
        void itShouldCallExpectedApi() throws IOException {
            restServiceServer.expect(MockRestRequestMatchers.requestTo(
                            "https://oris.orientacnisporty.cz/API/?format=json&method=getEvent&id=12345"))
                    .andRespond(withJsonResponseHavingBodyFromResourceFile(200, "oris/getEventDetailsResponse.json"));

            testedClient.getEventDetails(12345);

            restServiceServer.verify();
        }
    }


    @DisplayName("getEventEntries API tests")
    @Nested
    class GetEventEntriesApiTests {
        @Test
        @DisplayName("it should return parsed data from response")
        void itShouldReturnExpectedData() throws IOException {
            restServiceServer.expect(MockRestRequestMatchers.anything())
                    .andRespond(withJsonResponseHavingBodyFromResourceFile(200, "oris/getEventEntriesResponse.json"));

            var actualResponse = testedClient.getEventEntries(2077, 205);

            Map<String, EventEntry> expected = Map.of(
                    "Entry_14000",
                    EventEntryBuilder.builder()
                            .id(14000)
                            .classId(39529)
                            .classDesc("H20C")
                            .regNo("AOV9401")
                            .firstName("Marek")
                            .lastName("Rohel")
                            .si(1625050)
                            .userId(14263)
                            .clubId(7)
                            .note("")
                            .fee(70)
                            .createdByUserId(14263)
                            .updatedByUserId(2553)
                            .build(),
                    "Entry_17952",
                    EventEntryBuilder.builder()
                            .id(17952)
                            .classId(39533)
                            .classDesc("H55C")
                            .regNo("OOP5001")
                            .firstName("Břetislav")
                            .lastName("Ševčík")
                            .si(49301)
                            .userId(3418)
                            .clubId(104)
                            .note("")
                            .fee(70)
                            .createdByUserId(3418)
                            .updatedByUserId(3418)
                            .build()
            );

            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.status()).isEqualTo("OK");
            assertThat(actualResponse.format()).isEqualTo("json");
            assertThat(actualResponse.method()).isEqualTo("getEventEntries");
            assertThat(actualResponse.data()).isNotNull();
            assertThat(actualResponse.data())
                    .usingRecursiveComparison()
                    .ignoringExpectedNullFields()
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("it should call expected API with parameters")
        void itShouldCallExpectedApi() throws IOException {
            restServiceServer.expect(MockRestRequestMatchers.requestTo(
                            "https://oris.orientacnisporty.cz/API/?format=json&method=getEventEntries&eventid=2077&clubid=205"))
                    .andRespond(withJsonResponseHavingBodyFromResourceFile(200, "oris/getEventEntriesResponse.json"));

            testedClient.getEventEntries(2077, 205);

            restServiceServer.verify();
        }
    }

}