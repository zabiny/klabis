package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.members.MemberId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.application.FeeYearLifecyclePort;
import com.klabis.membershipfees.application.MemberFeeHistoryPort;
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

    private final FeeYearLifecyclePort feeYearLifecyclePort;
    private final MemberFeeHistoryPort memberFeeHistoryPort;

    MemberFeeChoiceController(FeeYearLifecyclePort feeYearLifecyclePort, MemberFeeHistoryPort memberFeeHistoryPort) {
        this.feeYearLifecyclePort = feeYearLifecyclePort;
        this.memberFeeHistoryPort = memberFeeHistoryPort;
    }

    @Override
    public ResponseEntity<MemberFeeChoiceResponse> getChoice(
            UUID memberId,
            Integer year) {

        MemberId memberIdObj = new MemberId(memberId);
        Optional<MembershipFeeGroupId> currentChoice = memberFeeHistoryPort.getCurrentChoice(memberIdObj, year);
        Optional<MembershipFeeTierId> recommended = memberFeeHistoryPort.getRecommendedLevelForYear(memberIdObj, year);

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

        feeYearLifecyclePort.chooseFeeLevel(new FeeYearLifecyclePort.ChooseFeeLevel(
                new MemberId(memberId),
                new MembershipFeeGroupId(request.membershipFeeGroupId()),
                year));

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> removeChoice(
            UUID memberId,
            Integer year) {

        feeYearLifecyclePort.removeFeeChoice(new MemberId(memberId), year);

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
