package club.klabis.adapters.oris;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RestClientTest(OrisConfiguration.class)
class OrisApiClientTest {

    @Autowired
    OrisApiClient testedClient;

    @Autowired
    MockRestServiceServer restServiceServer;

    // ORIS returns [] in Data when member is not found (instead of {} or null) and that leads into wrong value parsed. This test makes sure that "workaround" what is in place for that case in configured API client works properly and response returns OrisResponse with Data=null
    @Test
    void checkMemberNotFoundHandling() throws IOException {
        final String jsonBody = new ClassPathResource("oris/memberNotFoundResponse.json")
                .getContentAsString(StandardCharsets.UTF_8);
        restServiceServer.expect(MockRestRequestMatchers.queryParam("rgnum", "32323"))
                .andRespond(withStatus(HttpStatusCode.valueOf(200)).body(jsonBody).contentType(MediaType.parseMediaType("application/javascript")));

        OrisApiClient.OrisResponse<OrisApiClient.OrisUserInfo> actualResponse = testedClient.getUserInfo("32323");

        assertThat(actualResponse.data()).isNull();
    }

}