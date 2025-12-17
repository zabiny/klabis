package club.klabis.calendar.infrastructure;

import club.klabis.adapters.api.ApiTestConfiguration;
import club.klabis.calendar.CalendarItem;
import club.klabis.calendar.CreateCalendarItemCommand;
import club.klabis.shared.config.Globals;
import club.klabis.shared.config.hateoas.RootController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DisplayName("Calendar API tests")
@ApiTestConfiguration(controllers = {CalendarApiController.class, RootController.class})
@WithMockUser
@Import(CalendarRootPostprocessor.class)
class CalendarApiControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @MockitoBean
    private CalendarService calendarServiceMock;

    @DisplayName("GET /calendar tests")
    @Nested
    class GetCalendarItemsTests {

        @DisplayName("it should pass correct parameters to service call")
        @Test
        void itShouldPassCorrectParamsToService() {
            mockMvcTester.perform(get("/calendar-items")
                            .queryParam("calendarType", "MONTH"))
                    .assertThat()
                    .hasStatus(HttpStatus.OK);
        }

        @DisplayName("it should have 'createCalendarItem' affordance in HAL+FORMS response")
        @Test
        void itShouldAddExpectedLinkToRootNavigation() {
            mockMvcTester.perform(get("/calendar-items").accept(MediaTypes.HAL_FORMS_JSON))
                    .assertThat()
                    .hasStatus(HttpStatus.OK)
                    .bodyJson()
                    .extractingPath("_templates")
                    .hasFieldOrProperty("createCalendarItem");
        }

    }

    @DisplayName("GET /calendar-items/{id} tests")
    @Nested
    class GetCalendarItemByIdTests {
        @DisplayName("it should return HTTP 200 with requested calendar item")
        @Test
        void itShouldPassCorrectParamsToService() {
            CalendarItem returnedItem = CalendarItem.calendarItem(Globals.createZonedDateTime(2020, 10, 2),
                    Globals.createZonedDateTime(2020, 12, 5)).withNote("Example task");

            when(calendarServiceMock.getCalendarItem(CalendarItem.id(2))).thenReturn(Optional.of(returnedItem));

            mockMvcTester.perform(get("/calendar-items/2")
                            .queryParam("calendarType", "MONTH"))
                    .assertThat()
                    .hasStatus(HttpStatus.OK)
                    .bodyJson()
                    .isLenientlyEqualTo("""
                            {"start":"2020-10-02", "end": "2020-12-05", "note":"Example task"}
                            """.formatted(returnedItem.getId().value()));
        }

        @DisplayName("it should return HTTP 404 when calendar item doesn't exist")
        @Test
        void itShouldRespondHttp404() {
            mockMvcTester.perform(get("/calendar-items/2")
                            .queryParam("calendarType", "MONTH"))
                    .assertThat()
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .convertTo(ProblemDetail.class)
                    .extracting(ProblemDetail::getDetail, ProblemDetail::getStatus)
                    .containsExactly("Calendar item with id CALENDARITEM_2 was not found", 404);
        }

    }

    @DisplayName("POST /calendar-items/{id} tests")
    @Nested
    class PostCalendarItemByIdTests {

        @DisplayName("it should call service method with expected arguments")
        @Test
        void itShouldPassCorrectParametersToService() {
            mockMvcTester.perform(post("/calendar-items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"start": "2020-12-01", "end": "2020-12-12", "note":"Budou vanoce"}
                            """));

            CreateCalendarItemCommand expectedCommand = new CreateCalendarItemCommand(LocalDate.of(2020, 12, 1),
                    LocalDate.of(2020, 12, 12),
                    "Budou vanoce");
            verify(calendarServiceMock).createCalendarItem(expectedCommand);
        }

        @DisplayName("it should return expected HTTP 201 response data for success")
        @Test
        void itShouldReturnExpectedData() {
            CalendarItem item = CalendarItem.calendarItem(
                            Globals.createZonedDateTime(2020, 12, 1),
                            Globals.createZonedDateTime(2020, 12, 12))
                    .withNote("Budou vanoce");
            when(calendarServiceMock.createCalendarItem(any(CreateCalendarItemCommand.class)))
                    .thenReturn(item);

            mockMvcTester.perform(post("/calendar-items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"start": "2020-12-01", "end": "2020-12-12", "note":"Budou vanoce"}
                                    """))
                    .assertThat()
                    .hasStatus(HttpStatus.CREATED)
                    .hasHeader(HttpHeaders.LOCATION, "http://localhost/calendar-items/" + item.getId().value());
        }

    }

    @DisplayName("Root hateoas resource tests")
    @Nested
    class RootHateoasResourceTests {

        @DisplayName("it should add expected link to root navigation")
        @Test
        void itShouldAddExpectedLinkToRootNavigation() {
            mockMvcTester.perform(get("/").accept(MediaTypes.HAL_JSON_VALUE))
                    .assertThat()
                    .hasStatus(HttpStatus.OK)
                    .bodyJson()
                    .convertTo(EntityModel.class)
                    .extracting(e -> e.getLinks().toList())
                    .asList()
                    .containsExactly(Link.of("http://localhost/calendar-items?calendarType=&referenceDate=",
                            "calendar"));
        }

    }

}