package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.users.Authority;
import com.klabis.members.ActingMember;
import com.klabis.members.MemberId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.application.MemberChoicePort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "MemberFeeChoice", description = "Member fee level choice API")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.MEMBERS_SCOPE})
class MemberFeeChoiceController implements MemberFeeChoiceApi {

    private final MemberChoicePort memberChoicePort;

    MemberFeeChoiceController(MemberChoicePort memberChoicePort) {
        this.memberChoicePort = memberChoicePort;
    }

    // No @HasAuthority / @OwnerVisible on any of this controller's operations: authorization is
    // enforced imperatively below via assertMemberAccessingSelf — ONLY the member themselves, no
    // MANAGE-authority alternative. Stricter than @OwnerVisible's OR-with-authority semantics, so
    // converting this to x-klabis-owner-visible in the spec would widen access. See
    // membershipfees.yaml header comment.
    @Override
    @Operation(summary = "Get member's current fee level choice for a year")
    public ResponseEntity<MemberFeeChoiceResponse> getChoice(
            @Parameter(description = "Member UUID") UUID memberId,
            @Parameter(description = "Calendar year") Integer year,
            @ActingMember MemberId actingMember) {

        assertMemberAccessingSelf(memberId, actingMember);

        MemberId memberIdObj = new MemberId(memberId);
        Optional<MembershipFeeGroupId> currentChoice = memberChoicePort.getCurrentChoice(memberIdObj, year);
        Optional<MembershipFeeTierId> recommended = memberChoicePort.getRecommendedLevelForYear(memberIdObj, year);

        MemberFeeChoiceResponse response = MemberFeeChoiceResponse.of(memberId, year, currentChoice, recommended);
        HalResponseContext.setDomain(new FeeChoiceView(memberId, year, currentChoice, recommended));
        return ResponseEntity.ok(response);
    }

    // No real aggregate backs this projection — mirrors MemberAccountController's use of MemberId
    // as its postprocessor's "domain" type for the same reason (a purely computed response).
    record FeeChoiceView(UUID memberId, int year, Optional<MembershipFeeGroupId> currentChoice,
                         Optional<MembershipFeeTierId> recommended) {
    }

    @Override
    @Operation(summary = "Choose a fee level for a year")
    public ResponseEntity<Void> chooseTier(
            @Parameter(description = "Member UUID") UUID memberId,
            @Parameter(description = "Calendar year") Integer year,
            @RequestBody ChooseFeeChoiceRequest request,
            @ActingMember MemberId actingMember) {

        assertMemberAccessingSelf(memberId, actingMember);

        memberChoicePort.chooseFeeLevel(new MemberChoicePort.ChooseFeeLevel(
                new MemberId(memberId),
                new MembershipFeeGroupId(request.membershipFeeGroupId()),
                year));

        return ResponseEntity.noContent().build();
    }

    @Override
    @Operation(summary = "Remove fee level choice for a year")
    public ResponseEntity<Void> removeChoice(
            @Parameter(description = "Member UUID") UUID memberId,
            @Parameter(description = "Calendar year") Integer year,
            @ActingMember MemberId actingMember) {

        assertMemberAccessingSelf(memberId, actingMember);

        memberChoicePort.removeFeeChoice(new MemberId(memberId), year);

        return ResponseEntity.noContent().build();
    }

    private void assertMemberAccessingSelf(UUID memberId, MemberId actingMember) {
        if (!actingMember.value().equals(memberId)) {
            throw new AccessDeniedException("Members can only manage their own fee level choice");
        }
    }
}

@MvcComponent
class MemberFeeChoiceDetailsPostprocessor
        extends ModelWithDomainPostprocessor<MemberFeeChoiceResponse, MemberFeeChoiceController.FeeChoiceView> {

    @Override
    public void process(EntityModel<MemberFeeChoiceResponse> dtoModel, MemberFeeChoiceController.FeeChoiceView view) {
        klabisLinkTo(methodOn(MemberFeeChoiceController.class).getChoice(view.memberId(), view.year(), null))
                .ifPresent(link -> dtoModel.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(MemberFeeChoiceController.class)
                                .chooseTier(view.memberId(), view.year(), null, null)))
                        .andAffordances(klabisAfford(methodOn(MemberFeeChoiceController.class)
                                .removeChoice(view.memberId(), view.year(), null)))));

        view.currentChoice().ifPresent(groupId ->
                klabisLinkTo(methodOn(MembershipFeeGroupController.class).getGroup(groupId.value()))
                        .ifPresent(link -> dtoModel.add(link.withRel("currentGroup"))));

        view.recommended().ifPresent(levelId ->
                klabisLinkTo(methodOn(MembershipFeeTierController.class).getTier(levelId.value()))
                        .ifPresent(link -> dtoModel.add(link.withRel("recommendedLevel"))));
    }
}
