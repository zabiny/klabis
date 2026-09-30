package com.klabis.members.legalguardiangroup.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.users.Authority;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.LegalGuardianGroupsApi;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupFilter;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupRepository;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@MvcComponent
public class MemberLegalGuardianGroupLinkProcessor implements RepresentationModelProcessor<EntityModel<MemberDetailsResponse>> {

    private static final int ADULT_AGE = 18;

    private final LegalGuardianGroupRepository legalGuardianGroupRepository;

    MemberLegalGuardianGroupLinkProcessor(LegalGuardianGroupRepository legalGuardianGroupRepository) {
        this.legalGuardianGroupRepository = legalGuardianGroupRepository;
    }

    @Override
    public EntityModel<MemberDetailsResponse> process(EntityModel<MemberDetailsResponse> model) {
        MemberDetailsResponse content = model.getContent();
        MemberId minorId = new MemberId(content.id());
        legalGuardianGroupRepository.findOne(LegalGuardianGroupFilter.all().withMinorIs(minorId.toUserId()))
                .ifPresent(group -> {
                    klabisLinkTo(methodOn(LegalGuardianGroupsApi.class).getLegalGuardianGroup(group.getId().uuid()))
                            .map(link -> link.withRel("legalGuardianGroup"))
                            .ifPresent(model::add);
                    if (isMinor(content) && mayViewGuardians(content.id())) {
                        addGuardiansLink(model, group);
                    }
                });
        return model;
    }

    private static void addGuardiansLink(EntityModel<MemberDetailsResponse> model, LegalGuardianGroup group) {
        klabisLinkTo(methodOn(LegalGuardianGroupsApi.class).listLegalGuardianGroupGuardians(group.getId().uuid(), null))
                .map(link -> link.withRel("legalGuardians"))
                .ifPresent(model::add);
    }

    // The minor may have turned 18 before the daily age-out job removed them from the group.
    private static boolean isMinor(MemberDetailsResponse content) {
        LocalDate dateOfBirth = content.dateOfBirth();
        return dateOfBirth != null && Period.between(dateOfBirth, LocalDate.now()).getYears() < ADULT_AGE;
    }

    private static boolean mayViewGuardians(UUID minorId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        boolean canManage = authentication.getAuthorities().stream()
                .anyMatch(authority -> Authority.MEMBERS_MANAGE.getValue().equals(authority.getAuthority()));
        boolean isTheMinor = authentication instanceof KlabisJwtAuthenticationToken token
                             && token.getMemberIdUuid().map(minorId::equals).orElse(false);
        return canManage || isTheMinor;
    }
}
