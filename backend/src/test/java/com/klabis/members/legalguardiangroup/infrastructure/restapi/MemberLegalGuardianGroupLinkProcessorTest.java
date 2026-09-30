package com.klabis.members.legalguardiangroup.infrastructure.restapi;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponseBuilder;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Guardian;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Minor;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupFilter;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupRepository;
import com.klabis.common.security.fieldsecurity.OwnershipResolver;
import com.klabis.common.ui.HalFormsSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The processor reaches the groups from a member detail, where only a member id is available.
 * Legal guardian groups are keyed on {@code UserId}, so the member id has to be converted before the
 * lookup. The stubbed repository answers the way the JDBC adapter does — by the group's minors — so a
 * wrong id in the filter yields no group.
 */
@DisplayName("MemberLegalGuardianGroupLinkProcessor")
@ExtendWith(MockitoExtension.class)
class MemberLegalGuardianGroupLinkProcessorTest {

    private static final MemberId CHILD = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final MemberId OTHER_CHILD = new MemberId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    private static final MemberId GUARDIAN = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    @Mock
    private LegalGuardianGroupRepository legalGuardianGroupRepository;

    private MemberLegalGuardianGroupLinkProcessor processor;

    @BeforeEach
    void setUp() throws Exception {
        processor = new MemberLegalGuardianGroupLinkProcessor(legalGuardianGroupRepository);
        @SuppressWarnings("unchecked")
        ObjectProvider<OwnershipResolver> ownershipResolver = mock(ObjectProvider.class);
        Field instance = HalFormsSupport.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        instance.set(null, new HalFormsSupport(ownershipResolver));
        authenticateWith("MEMBERS:MANAGE");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateWith(String authority) {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().doReturn(List.of(new SimpleGrantedAuthority(authority))).when(authentication).getAuthorities();
        SecurityContext context = mock(SecurityContext.class);
        lenient().when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
    }

    private void givenGroups(LegalGuardianGroup... groups) {
        when(legalGuardianGroupRepository.findOne(any(LegalGuardianGroupFilter.class))).thenAnswer(invocation -> {
            UserId userId = invocation.<LegalGuardianGroupFilter>getArgument(0).minorIs();
            if (userId == null) {
                return Optional.empty();
            }
            return Arrays.stream(groups).filter(g -> g.hasMember(userId)).findFirst();
        });
    }

    private static LegalGuardianGroup groupOfChild(MemberId child) {
        return LegalGuardianGroup.create(Set.of(new Guardian(GUARDIAN.toUserId(), "Novák")),
                new Minor(child, LocalDate.now().minusYears(9)));
    }

    private static EntityModel<MemberDetailsResponse> detailOf(MemberId memberId) {
        return EntityModel.of(MemberDetailsResponseBuilder.builder().id(memberId.uuid()).build());
    }

    @Test
    @DisplayName("adds legalGuardianGroup link pointing at the group the minor belongs to")
    void addsLinkForMinor() {
        LegalGuardianGroup group = groupOfChild(CHILD);
        givenGroups(group);
        EntityModel<MemberDetailsResponse> model = detailOf(CHILD);

        processor.process(model);

        Optional<Link> link = model.getLink("legalGuardianGroup");
        assertThat(link).isPresent();
        assertThat(link.get().getHref()).endsWith("/api/legal-guardian-groups/" + group.getId().uuid());
    }

    @Test
    @DisplayName("looks the group up by the minor's own user id")
    void looksUpByMinorUserId() {
        givenGroups(groupOfChild(CHILD));

        processor.process(detailOf(CHILD));

        ArgumentCaptor<LegalGuardianGroupFilter> captor = ArgumentCaptor.forClass(LegalGuardianGroupFilter.class);
        verify(legalGuardianGroupRepository).findOne(captor.capture());
        assertThat(captor.getValue().minorIs()).isEqualTo(CHILD.toUserId());
    }

    @Test
    @DisplayName("adds no link when the caller lacks MEMBERS:MANAGE")
    void addsNoLinkWithoutManageAuthority() {
        authenticateWith("MEMBERS:READ");
        givenGroups(groupOfChild(CHILD));
        EntityModel<MemberDetailsResponse> model = detailOf(CHILD);

        processor.process(model);

        assertThat(model.getLink("legalGuardianGroup")).isEmpty();
    }

    @Test
    @DisplayName("adds no link when the member is not a minor in any group")
    void addsNoLinkWithoutGroup() {
        givenGroups(groupOfChild(OTHER_CHILD));
        EntityModel<MemberDetailsResponse> model = detailOf(CHILD);

        processor.process(model);

        assertThat(model.getLink("legalGuardianGroup")).isEmpty();
    }

    @Test
    @DisplayName("adds no link on the detail of a guardian")
    void addsNoLinkForGuardian() {
        givenGroups(groupOfChild(CHILD));
        EntityModel<MemberDetailsResponse> model = detailOf(GUARDIAN);

        processor.process(model);

        assertThat(model.getLink("legalGuardianGroup")).isEmpty();
    }
}
