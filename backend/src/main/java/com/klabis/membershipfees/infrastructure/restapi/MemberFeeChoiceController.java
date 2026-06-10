package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.users.Authority;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.application.MemberChoicePort;
import com.klabis.members.ActingMember;
import com.klabis.members.MemberId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/members/{memberId}/fee-choice", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "MemberFeeChoice", description = "Member fee level choice API")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.MEMBERS_SCOPE})
class MemberFeeChoiceController {

    private final MemberChoicePort memberChoicePort;

    MemberFeeChoiceController(MemberChoicePort memberChoicePort) {
        this.memberChoicePort = memberChoicePort;
    }

    @GetMapping("/{year}")
    @Operation(summary = "Get member's current fee level choice for a year")
    ResponseEntity<EntityModel<MemberFeeChoiceResponse>> getChoice(
            @Parameter(description = "Member UUID") @PathVariable UUID memberId,
            @Parameter(description = "Calendar year") @PathVariable int year,
            @ActingMember MemberId actingMember) {

        assertMemberAccessingSelf(memberId, actingMember);

        MemberId memberIdObj = new MemberId(memberId);
        Optional<MembershipFeeGroupId> currentChoice = memberChoicePort.getCurrentChoice(memberIdObj, year);
        Optional<MembershipFeeLevelId> recommended = memberChoicePort.getRecommendedLevelForYear(memberIdObj, year);

        MemberFeeChoiceResponse response = MemberFeeChoiceResponse.of(memberId, year, currentChoice, recommended);
        EntityModel<MemberFeeChoiceResponse> model = EntityModel.of(response);

        klabisLinkTo(methodOn(MemberFeeChoiceController.class).getChoice(memberId, year, null))
                .ifPresent(link -> model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(MemberFeeChoiceController.class)
                                .chooseLevel(memberId, year, null, null)))
                        .andAffordances(klabisAfford(methodOn(MemberFeeChoiceController.class)
                                .removeChoice(memberId, year, null)))));

        currentChoice.ifPresent(groupId ->
                klabisLinkTo(methodOn(MembershipFeeGroupController.class).getGroup(groupId.value()))
                        .ifPresent(link -> model.add(link.withRel("currentGroup"))));

        recommended.ifPresent(levelId ->
                klabisLinkTo(methodOn(MembershipFeeLevelController.class).getLevel(levelId.value()))
                        .ifPresent(link -> model.add(link.withRel("recommendedLevel"))));

        return ResponseEntity.ok(model);
    }

    @PostMapping(value = "/{year}", consumes = "application/json")
    @Operation(summary = "Choose a fee level for a year")
    ResponseEntity<Void> chooseLevel(
            @Parameter(description = "Member UUID") @PathVariable UUID memberId,
            @Parameter(description = "Calendar year") @PathVariable int year,
            @Valid @RequestBody ChooseFeeChoiceRequest request,
            @ActingMember MemberId actingMember) {

        assertMemberAccessingSelf(memberId, actingMember);

        memberChoicePort.chooseFeeLevel(new MemberChoicePort.ChooseFeeLevel(
                new MemberId(memberId),
                new MembershipFeeGroupId(request.membershipFeeGroupId()),
                year));

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{year}")
    @Operation(summary = "Remove fee level choice for a year")
    ResponseEntity<Void> removeChoice(
            @Parameter(description = "Member UUID") @PathVariable UUID memberId,
            @Parameter(description = "Calendar year") @PathVariable int year,
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
