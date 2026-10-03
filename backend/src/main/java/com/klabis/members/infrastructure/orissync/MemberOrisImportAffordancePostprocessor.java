package com.klabis.members.infrastructure.orissync;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.settings.OrisClubKeyManagementPort;
import com.klabis.members.application.MemberDiscoveryPort;
import java.util.Optional;
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
 * Lives in {@code orissync} rather than alongside {@code MemberListPostprocessor} in
 * {@code restapi} because the import affordance is an ORIS-integration concern, not a plain
 * member-listing one — only the profile already gated on {@code oris} changes the plain list
 * shape. The port it reads the key state for now lives in {@code common.settings}.
 */
@MvcComponent
public class MemberOrisImportAffordancePostprocessor implements RepresentationModelProcessor<PagedModel<EntityModel<MemberSummaryResponse>>> {

    private final Optional<MemberDiscoveryPort> memberDiscoveryPort;
    private final OrisClubKeyManagementPort orisClubKeyManagementPort;

    public MemberOrisImportAffordancePostprocessor(Optional<MemberDiscoveryPort> memberDiscoveryPort,
                                                   OrisClubKeyManagementPort orisClubKeyManagementPort) {
        this.memberDiscoveryPort = memberDiscoveryPort;
        this.orisClubKeyManagementPort = orisClubKeyManagementPort;
    }

    @Override
    public PagedModel<EntityModel<MemberSummaryResponse>> process(PagedModel<EntityModel<MemberSummaryResponse>> pagedModel) {
        boolean clubKeyHeld = memberDiscoveryPort.isPresent() && orisClubKeyManagementPort.isSet();
        if (!clubKeyHeld) {
            return pagedModel;
        }
        pagedModel.mapLink(IanaLinkRelations.SELF, selfLink -> (Link) selfLink
                .andAffordances(klabisAfford(methodOn(MembersApi.class).importFromOris())));
        return pagedModel;
    }
}
