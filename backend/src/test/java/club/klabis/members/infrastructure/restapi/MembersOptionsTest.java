package club.klabis.members.infrastructure.restapi;

import club.klabis.adapters.api.ApiTestConfiguration;
import club.klabis.adapters.api.WithKlabisUserMocked;
import club.klabis.members.MemberId;
import club.klabis.members.application.MembersRepository;
import club.klabis.members.domain.Contact;
import club.klabis.members.domain.Member;
import club.klabis.members.domain.RegistrationNumber;
import club.klabis.members.domain.forms.RegistrationForm;
import club.klabis.members.domain.forms.RegistrationFormBuilder;
import club.klabis.members.infrastructure.restapi.dto.MembersApiResponse;
import club.klabis.shared.config.hateoas.ModelPreparator;
import club.klabis.shared.config.security.KlabisSecurityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for GET /members/options endpoint
 * <p>
 * Verifies that members are returned as HAL+Forms options with correct format:
 * {value: memberId, prompt: "FirstName LastName (RegistrationNumber)"}
 */
@ApiTestConfiguration(controllers = MembersApi.class)
@DisplayName("GET /members/options endpoint")
class MembersOptionsTest {

    @MockitoBean
    private MembersRepository membersRepositoryMock;

    @MockitoBean
    private ModelPreparator<Member, MembersApiResponse> memberModelAssemblerMock;

    @MockitoBean
    private PagedResourcesAssembler<Member> pagedResourcesAssemblerMock;

    @MockitoBean
    private KlabisSecurityService klabisSecurityServiceMock;

    @Autowired
    private MockMvc mockMvc;

    /**
     * Creates a test member with specified details
     */
    private Member createMember(String firstName, String lastName, String registrationId, int memberId) {
        RegistrationForm form = RegistrationFormBuilder.builder()
                .firstName(firstName)
                .lastName(lastName)
                .registrationNumber(RegistrationNumber.ofRegistrationId(registrationId))
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .contact(List.of(
                        Contact.email(firstName.toLowerCase() + "@test.com", "personal email"),
                        Contact.phone("+420123456789", "mobile")
                ))
                .build();
        Member member = Member.fromRegistration(form);
        ReflectionTestUtils.setField(member, "id", new MemberId(memberId));
        return member;
    }

    @Test
    @WithKlabisUserMocked
    @DisplayName("should return members as options with correct format")
    void shouldReturnMembersAsOptions() throws Exception {
        // Arrange
        Member member1 = createMember("John", "Smith", "ZBM9000", 1);
        Member member2 = createMember("Jane", "Doe", "ZBM9001", 2);

        when(membersRepositoryMock.findAllBySuspended(eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(member1, member2)));

        // Act & Assert
        mockMvc.perform(get("/members/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value(1))
                .andExpect(jsonPath("$[0].prompt").value("John Smith (ZBM9000)"))
                .andExpect(jsonPath("$[1].value").value(2))
                .andExpect(jsonPath("$[1].prompt").value("Jane Doe (ZBM9001)"));
    }

    @Test
    @WithKlabisUserMocked
    @DisplayName("should return only active members (suspended=false)")
    void shouldReturnOnlyActiveMembers() throws Exception {
        // Arrange
        Member activeMember = createMember("Active", "Member", "ZBM9000", 1);

        when(membersRepositoryMock.findAllBySuspended(eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(activeMember)));

        // Act & Assert
        mockMvc.perform(get("/members/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].prompt").value("Active Member (ZBM9000)"));
    }

    @Test
    @WithKlabisUserMocked
    @DisplayName("should return empty list when no active members exist")
    void shouldReturnEmptyListWhenNoMembers() throws Exception {
        // Arrange
        when(membersRepositoryMock.findAllBySuspended(eq(false), any()))
                .thenReturn(new PageImpl<>(List.of()));

        // Act & Assert
        mockMvc.perform(get("/members/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithKlabisUserMocked
    @DisplayName("should handle members with null registration gracefully")
    void shouldHandleNullRegistration() throws Exception {
        // Arrange
        Member memberNoRegistration = createMember("Name", "Only", "ZBM9000", 1);
        ReflectionTestUtils.setField(memberNoRegistration, "registration", null);

        when(membersRepositoryMock.findAllBySuspended(eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(memberNoRegistration)));

        // Act & Assert
        mockMvc.perform(get("/members/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value(1))
                .andExpect(jsonPath("$[0].prompt").value("Name Only (N/A)"));
    }

    @Test
    @DisplayName("should require authentication")
    void shouldRequireAuthentication() throws Exception {
        // Endpoint requires authentication - unauthenticated requests should get 401
        mockMvc.perform(get("/members/options"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithKlabisUserMocked
    @DisplayName("should return all members regardless of count (no pagination limit)")
    void shouldReturnAllMembersWithoutLimit() throws Exception {
        // Arrange - create 250 members to test that limit is removed (original was 200)
        // Use registration numbers with year "90" to match birth date 1990
        List<Member> members = IntStream.rangeClosed(1, 250)
                .mapToObj(i -> {
                    // Format: ZBM + year (90 for 1990) + order (01-99, then wrap with different year)
                    int order = ((i - 1) % 99) + 1;
                    String regNum = "ZBM90" + String.format("%02d", order);
                    return createMember("First" + i, "Last" + i, regNum, i);
                })
                .toList();

        when(membersRepositoryMock.findAllBySuspended(eq(false), any()))
                .thenReturn(new PageImpl<>(members));

        // Act & Assert
        mockMvc.perform(get("/members/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(250));
    }
}
