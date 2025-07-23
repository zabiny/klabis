package club.klabis.oris.application.apiclient;

import club.klabis.oris.application.apiclient.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RestClientTest(OrisConfiguration.class)
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

            OrisApiClient.OrisResponse<Map<String, OrisEvent>> expectedData = new OrisApiClient.OrisResponse<>(
                    Map.of("Event_9160", new OrisEvent(
                            9160,
                            "Západočeský žebříček - jaro 2025",
                            LocalDate.of(2025, 1, 1),
                            "",
                            new OrisEventOrg(
                                    646, "ZCO", "Západočeská oblast"
                            ),
                            new OrisEventLevel(
                                    4, "OŽ", "Oblastní žebříček", "Local event"
                            ),
                            new OrisEventSport(
                                    1, "OB", "Foot O"
                            ),
                            new OrisEventDiscipline(
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
}