package com.klabis.members.infrastructure.orissync;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.members.infrastructure.restapi.MemberSummaryResponse;
import com.klabis.members.infrastructure.restapi.MembersApi;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Adds the importFromOris affordance to the member list's self link, but only while a club key
 * is held (design.md D11) — without one the action could not do anything, so it must not be
 * offered even to a caller holding SYNC:MANAGE. {@code klabisAfford} already withholds the
 * affordance from a caller lacking SYNC:MANAGE, so this postprocessor only adds the second,
 * state-dependent condition.
 * <p>
 * Reads the key state from {@link HalResponseContext} rather than injecting {@link OrisClubKeyPort}
 * directly, mirroring {@code DisciplineController}'s {@code EnrolledDisciplineIds} (design.md D9
 * there): {@code @MvcComponent} beans are scanned into every {@code @WebMvcTest} slice in the
 * application, so a constructor dependency here would force every unrelated controller test to
 * mock {@link OrisClubKeyPort} too.
 * <p>
 * Lives in {@code orissync} rather than alongside {@code MemberListPostprocessor} in
 * {@code restapi} because it is the {@code orissync} package that owns {@link OrisClubKeyPort}.
 */
@MvcComponent
public class MemberOrisImportAffordancePostprocessor implements RepresentationModelProcessor<PagedModel<EntityModel<MemberSummaryResponse>>> {

    @Override
    public PagedModel<EntityModel<MemberSummaryResponse>> process(PagedModel<EntityModel<MemberSummaryResponse>> pagedModel) {
        boolean clubKeyHeld = HalResponseContext.findContext(ClubKeyHeld.class)
                .map(ClubKeyHeld::held)
                .orElse(false);
        if (!clubKeyHeld) {
            return pagedModel;
        }
        pagedModel.mapLink(IanaLinkRelations.SELF, selfLink -> (Link) selfLink
                .andAffordances(klabisAfford(methodOn(MembersApi.class).importFromOris())));
        return pagedModel;
    }
}
