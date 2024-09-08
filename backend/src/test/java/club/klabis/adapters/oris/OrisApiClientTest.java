package club.klabis.adapters.oris;

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
        return withStatus(HttpStatusCode.valueOf(status)).body(body).contentType(MediaType.parseMediaType("application/javascript"));
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

            OrisApiClient.OrisResponse<OrisApiClient.OrisUserInfo> actualResponse = testedClient.getUserInfo("32323");

            assertThat(actualResponse.data()).extracting("orisId", "firstName", "lastName").containsExactly(452, "John", "Doe");
            assertThat(actualResponse.status()).isEqualTo("OK");
            assertThat(actualResponse.format()).isEqualTo("json");
            assertThat(actualResponse.method()).isEqualTo("getUser");
        }

        @Test
        @DisplayName("it should call expected API with parameters")
        void itShouldCallExpectedApi() throws IOException {
            restServiceServer.expect(MockRestRequestMatchers.requestTo("https://oris.orientacnisporty.cz/API/?format=json&method=getUser&rgnum=32323"))
                    .andRespond(withJsonResponseHavingBodyFromResourceFile(200, "oris/getUserExampleResponse.json"));

            testedClient.getUserInfo("32323");

            restServiceServer.verify();
        }
    }
}