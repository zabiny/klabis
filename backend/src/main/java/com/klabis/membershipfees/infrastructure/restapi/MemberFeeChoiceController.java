package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.members.MemberId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.application.MemberChoicePort;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
// Authorization is declared in membershipfees.yaml and enforced on the generated interface.
class MemberFeeChoiceController implements MemberFeeChoiceApi {

    private final MemberChoicePort memberChoicePort;

    MemberFeeChoiceController(MemberChoicePort memberChoicePort) {
        this.memberChoicePort = memberChoicePort;
    }

    @Override
    public ResponseEntity<MemberFeeChoiceResponse> getChoice(
            UUID memberId,
            Integer year) {

        MemberId memberIdObj = new MemberId(memberId);
        Optional<MembershipFeeGroupId> currentChoice = memberChoicePort.getCurrentChoice(memberIdObj, year);
        Optional<MembershipFeeTierId> recommended = memberChoicePort.getRecommendedLevelForYear(memberIdObj, year);

        MemberFeeChoiceResponse response = MembershipFeesResponseMapper.toResponse(memberId, year, currentChoice, recommended);
        HalResponseContext.setDomain(new FeeChoiceView(memberId, year, currentChoice, recommended));
        return ResponseEntity.ok(response);
    }

    // No real aggregate backs this projection — mirrors MemberAccountController's use of MemberId
    // as its postprocessor's "domain" type for the same reason (a purely computed response).
    record FeeChoiceView(UUID memberId, int year, Optional<MembershipFeeGroupId> currentChoice,
                         Optional<MembershipFeeTierId> recommended) {
    }

    @Override
    public ResponseEntity<Void> chooseTier(
            UUID memberId,
            Integer year,
            ChooseFeeChoiceRequest request) {

        memberChoicePort.chooseFeeLevel(new MemberChoicePort.ChooseFeeLevel(
                new MemberId(memberId),
                new MembershipFeeGroupId(request.membershipFeeGroupId()),
                year));

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> removeChoice(
            UUID memberId,
            Integer year) {

        memberChoicePort.removeFeeChoice(new MemberId(memberId), year);

        return ResponseEntity.noContent().build();
    }
}

@MvcComponent
class MemberFeeChoiceDetailsPostprocessor
        extends ModelWithDomainPostprocessor<MemberFeeChoiceResponse, MemberFeeChoiceController.FeeChoiceView> {

    @Override
    public void process(EntityModel<MemberFeeChoiceResponse> dtoModel, MemberFeeChoiceController.FeeChoiceView view) {
        klabisLinkTo(methodOn(MemberFeeChoiceApi.class).getChoice(view.memberId(), view.year()))
                .ifPresent(link -> dtoModel.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(MemberFeeChoiceApi.class)
                                .chooseTier(view.memberId(), view.year(), null)))
                        .andAffordances(klabisAfford(methodOn(MemberFeeChoiceApi.class)
                                .removeChoice(view.memberId(), view.year())))));

        view.currentChoice().ifPresent(groupId ->
                klabisLinkTo(methodOn(MembershipFeeGroupsApi.class).getFeeGroup(groupId.value()))
                        .ifPresent(link -> dtoModel.add(link.withRel("currentGroup"))));

        view.recommended().ifPresent(levelId ->
                klabisLinkTo(methodOn(MembershipFeeTiersApi.class).getTier(levelId.value()))
                        .ifPresent(link -> dtoModel.add(link.withRel("recommendedLevel"))));
    }
}
