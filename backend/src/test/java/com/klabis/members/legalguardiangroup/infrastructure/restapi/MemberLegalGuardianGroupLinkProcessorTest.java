package com.klabis.members.legalguardiangroup.infrastructure.restapi;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponseBuilder;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Guardian;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Minor;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.security.fieldsecurity.OwnershipResolver;
import com.klabis.common.ui.HalFormsSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.domain.Member;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * The processor reads the member's legal guardian group, looked up once by the controller, from the
 * response context and adds the links and the guardians form that depend on it.
 */
@DisplayName("MemberLegalGuardianGroupLinkProcessor")
@ExtendWith(MockitoExtension.class)
class MemberLegalGuardianGroupLinkProcessorTest {

    private static final MemberId CHILD = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final MemberId OTHER_CHILD = new MemberId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    private static final MemberId GUARDIAN = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    private final MemberLegalGuardianGroupLinkProcessor processor = new MemberLegalGuardianGroupLinkProcessor();

    @BeforeEach
    void setUp() throws Exception {
        @SuppressWarnings("unchecked")
        ObjectProvider<OwnershipResolver> ownershipResolver = mock(ObjectProvider.class);
        Field instance = HalFormsSupport.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        instance.set(null, new HalFormsSupport(ownershipResolver));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        authenticateAs(null, "MEMBERS:MANAGE");
    }

    @AfterEach
    void clearState() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private static void authenticateAs(MemberId memberId, String authority) {
        KlabisJwtAuthenticationToken authentication = mock(KlabisJwtAuthenticationToken.class);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().doReturn(List.of(new SimpleGrantedAuthority(authority))).when(authentication).getAuthorities();
        lenient().when(authentication.getMemberIdUuid())
                .thenReturn(Optional.ofNullable(memberId).map(MemberId::uuid));
        SecurityContext context = mock(SecurityContext.class);
        lenient().when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
    }

    private static void givenGroup(LegalGuardianGroup group) {
        HalResponseContext.setContext(new MemberLegalGuardianGroup(group));
    }

    private static LegalGuardianGroup groupOfChild(MemberId child) {
        return LegalGuardianGroup.create(Set.of(new Guardian(GUARDIAN.toUserId(), "Novák")),
                new Minor(child, LocalDate.now().minusYears(9)));
    }

    private static Member memberOf(MemberId memberId, LocalDate dateOfBirth) {
        return MemberTestDataBuilder.aMemberWithId(memberId.uuid()).withDateOfBirth(dateOfBirth).build();
    }

    private static Member minor(MemberId memberId) {
        return memberOf(memberId, LocalDate.now().minusYears(9));
    }

    private EntityModel<MemberDetailsResponse> process(Member member) {
        EntityModel<MemberDetailsResponse> model = EntityModel.of(
                MemberDetailsResponseBuilder.builder().id(member.getId().uuid()).build(),
                Link.of("/api/members/" + member.getId().uuid()).withSelfRel());
        processor.process(model, member);
        return model;
    }

    @Test
    @DisplayName("adds legalGuardianGroup link pointing at the group the minor belongs to")
    void addsLinkForMinor() {
        LegalGuardianGroup group = groupOfChild(CHILD);
        givenGroup(group);

        EntityModel<MemberDetailsResponse> model = process(minor(CHILD));

        Optional<Link> link = model.getLink("legalGuardianGroup");
        assertThat(link).isPresent();
        assertThat(link.get().getHref()).endsWith("/api/legal-guardian-groups/" + group.getId().uuid());
    }

    @Test
    @DisplayName("adds no link when the caller lacks MEMBERS:MANAGE")
    void addsNoLinkWithoutManageAuthority() {
        authenticateAs(null, "MEMBERS:READ");
        givenGroup(groupOfChild(CHILD));

        assertThat(process(minor(CHILD)).getLink("legalGuardianGroup")).isEmpty();
    }

    @Test
    @DisplayName("adds no link when the member is not a minor in any group")
    void addsNoLinkWithoutGroup() {
        givenGroup(null);

        assertThat(process(minor(CHILD)).getLink("legalGuardianGroup")).isEmpty();
    }

    @Test
    @DisplayName("adds no link when the controller published no group")
    void addsNoLinkWithoutContext() {
        assertThat(process(minor(CHILD)).getLink("legalGuardianGroup")).isEmpty();
    }

    @Test
    @DisplayName("adds legalGuardians link to the guardians of the group for MEMBERS:MANAGE")
    void addsGuardiansLinkForAdmin() {
        LegalGuardianGroup group = groupOfChild(CHILD);
        givenGroup(group);

        EntityModel<MemberDetailsResponse> model = process(minor(CHILD));

        assertThat(model.getLink("legalGuardians")).hasValueSatisfying(link -> assertThat(link.getHref())
                .endsWith("/api/legal-guardian-groups/" + group.getId().uuid() + "/guardians"));
    }

    @Test
    @DisplayName("adds legalGuardians link on the minor's own detail without MEMBERS:MANAGE")
    void addsGuardiansLinkForTheMinorThemself() {
        authenticateAs(CHILD, "MEMBERS:READ");
        givenGroup(groupOfChild(CHILD));

        assertThat(process(minor(CHILD)).getLink("legalGuardians")).isPresent();
    }

    @Test
    @DisplayName("adds no legalGuardians link on another member's detail without MEMBERS:MANAGE")
    void addsNoGuardiansLinkForOthers() {
        authenticateAs(OTHER_CHILD, "MEMBERS:READ");
        givenGroup(groupOfChild(CHILD));

        assertThat(process(minor(CHILD)).getLink("legalGuardians")).isEmpty();
    }

    @Test
    @DisplayName("adds no legalGuardians link for a member who has turned 18 but is still in a group")
    void addsNoGuardiansLinkForAdult() {
        givenGroup(groupOfChild(CHILD));

        assertThat(process(memberOf(CHILD, LocalDate.now().minusYears(18))).getLink("legalGuardians")).isEmpty();
    }

    @Test
    @DisplayName("adds no legalGuardians link for a minor without a group")
    void addsNoGuardiansLinkWithoutGroup() {
        givenGroup(null);

        assertThat(process(minor(CHILD)).getLink("legalGuardians")).isEmpty();
    }

    @Test
    @DisplayName("offers the setMemberLegalGuardians form on the self link of a minor only")
    void offersFormForMinorOnly() {
        assertThat(process(minor(CHILD)).getRequiredLink("self").getAffordances()).isNotEmpty();
        assertThat(process(memberOf(CHILD, LocalDate.now().minusYears(30))).getRequiredLink("self").getAffordances())
                .isEmpty();
    }
}
