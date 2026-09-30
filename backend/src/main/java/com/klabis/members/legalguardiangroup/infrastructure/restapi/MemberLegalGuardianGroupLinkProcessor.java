package com.klabis.members.legalguardiangroup.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.members.CurrentUserData;
import com.klabis.members.domain.Member;
import com.klabis.members.infrastructure.restapi.LegalGuardianGroupsApi;
import com.klabis.members.infrastructure.restapi.LegalGuardianOptions;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAffordWithOptions;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@MvcComponent
public class MemberLegalGuardianGroupLinkProcessor extends ModelWithDomainPostprocessor<MemberDetailsResponse, Member> {

    @Override
    public void process(EntityModel<MemberDetailsResponse> dtoModel, Member member) {
        // The minor may have turned 18 before the daily age-out job removed them from the group.
        boolean minor = member.getPersonalInformation().isMinor();
        UUID memberId = member.getId().uuid();

        if (minor) {
            dtoModel.mapLink(IanaLinkRelations.SELF, self -> (Link) self.andAffordances(klabisAffordWithOptions(
                    methodOn(LegalGuardianGroupsApi.class).setMemberLegalGuardians(memberId, null),
                    LegalGuardianOptions.forLegalGuardiansField())));
        }

        HalResponseContext.findContext(MemberLegalGuardianGroup.class)
                .map(MemberLegalGuardianGroup::group)
                .ifPresent(group -> {
                    klabisLinkTo(methodOn(LegalGuardianGroupsApi.class).getLegalGuardianGroup(group.getId().uuid()))
                            .map(link -> link.withRel("legalGuardianGroup"))
                            .ifPresent(dtoModel::add);
                    if (minor && mayViewGuardians(group)) {
                        klabisLinkTo(methodOn(LegalGuardianGroupsApi.class)
                                .listLegalGuardianGroupGuardians(group.getId().uuid(), null))
                                .map(link -> link.withRel("legalGuardians"))
                                .ifPresent(dtoModel::add);
                    }
                });
    }

    private static boolean mayViewGuardians(LegalGuardianGroup group) {
        return CurrentUserData.from(SecurityContextHolder.getContext().getAuthentication())
                .map(user -> GuardianListAccess.permits(user, group))
                .orElse(false);
    }
}
