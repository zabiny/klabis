package club.klabis.users.infrastructure.restapi;

import club.klabis.adapters.api.ApiTestConfiguration;
import club.klabis.adapters.api.WithKlabisUserMocked;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.users.application.ApplicationUsersRepository;
import club.klabis.users.application.UserGrantsUpdateUseCase;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;

@ApiTestConfiguration(controllers = UserPermissionsApiController.class)
class UserPermissionsApiControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @MockitoBean
    private UserGrantsUpdateUseCase userGrantsUpdateUseCaseMock;

    @MockitoBean
    private ApplicationUsersRepository applicationUsersRepositoryMock;

    @Nested
    @DisplayName("GET /grant_options")
    class GetGrantsTests {

        MvcTestResult callGetGrants() {
            return mockMvcTester.get().uri("/grants").exchange();
        }

        @DisplayName("it should respond with HTTP 401 when no one is authenticated")
        @Test
        void itShouldReturnHttp401WhenNoUserAuthenticated() {
            callGetGrants()
                    .assertThat()
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }

        // output attributes expect 'value' and 'prompt' attributes in JSON
        record ExpectedOptionJsonStructure(String value, String prompt) {
        }

        @WithKlabisUserMocked
        @DisplayName("it should return data in correct format")
        @Test
        void itShouldReturnDataInCorrectFormat() {
            callGetGrants()
                    .assertThat()
                    .hasStatus(HttpStatus.OK)
                    .hasContentType(MediaType.APPLICATION_JSON_VALUE)
                    .bodyJson()
                    .hasNoNullFieldsOrProperties()
                    .extractingPath("$[?(@.value=='members:permissions')]")
                    .convertTo(InstanceOfAssertFactories.list(ExpectedOptionJsonStructure.class))
                    .isEqualTo(List.of(new ExpectedOptionJsonStructure("members:permissions",
                            "Může spravovat oprávnění v aplikaci")));
        }

        @WithKlabisUserMocked
        @DisplayName("it should return all application global grants")
        @Test
        void itShouldReturnAllApplicationGrants() {
            callGetGrants()
                    .assertThat()
                    .hasStatus(HttpStatus.OK)
                    .bodyJson()
                    .extractingPath("$[*].value")
                    .isEqualTo(ApplicationGrant.globalGrants().stream().map(ApplicationGrant::getGrantName).toList());
        }


    }

}