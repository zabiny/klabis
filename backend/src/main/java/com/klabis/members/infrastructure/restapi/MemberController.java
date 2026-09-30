package com.klabis.members.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.ActingUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import com.klabis.members.application.MemberAccountActivationPort;
import com.klabis.members.application.MemberCompletenessPort;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.application.MemberDiscoveryPort;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberFilter;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.infrastructure.restapi.MemberLegalGuardianGroup;
import com.klabis.common.settings.OrisClubKeyPort;
import com.klabis.members.infrastructure.orissync.ClubKeyHeld;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncTarget;
import com.klabis.sync.infrastructure.restapi.SyncApi;
import com.klabis.sync.infrastructure.restapi.SyncEntityTypeParam;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisAffordWithOptions;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@ExposesResourceFor(Member.class)
public class MemberController implements MembersApi {

    private final ManagementPort managementService;
    private final MemberRepository memberRepository;
    private final ConversionService conversionService;
    private final Optional<MemberDiscoveryPort> memberDiscoveryJob;
    private final OrisClubKeyPort orisClubKeyPort;
    private final Optional<SynchronizationPort> synchronizationPort;
    private final MemberCompletenessPort memberCompletenessPort;
    private final LegalGuardianGroupPort legalGuardianGroupPort;

    public MemberController(
            ManagementPort managementService,
            MemberRepository memberRepository,
            ConversionService conversionService,
            Optional<MemberDiscoveryPort> memberDiscoveryJob,
            OrisClubKeyPort orisClubKeyPort,
            Optional<SynchronizationPort> synchronizationPort,
            MemberCompletenessPort memberCompletenessPort,
            LegalGuardianGroupPort legalGuardianGroupPort) {
        this.memberCompletenessPort = memberCompletenessPort;
        this.legalGuardianGroupPort = legalGuardianGroupPort;
        this.managementService = managementService;
        this.memberRepository = memberRepository;
        this.conversionService = conversionService;
        this.memberDiscoveryJob = memberDiscoveryJob;
        this.orisClubKeyPort = orisClubKeyPort;
        this.synchronizationPort = synchronizationPort;
    }

    /**
     * Runs the exact same discovery pass {@link MemberDiscoveryJob}'s own cron runs
     * (design.md D11) — this only lets a human start it once, deliberately, before the
     * schedule is relied upon. Running it twice brings nobody in twice: already-paired
     * members are skipped identically either way.
     * <p>
     * The only implementation of {@link MemberDiscoveryPort}, {@code MemberDiscoveryJob},
     * only exists under the {@code oris} profile ({@code @OrisIntegrationComponent});
     * without it there is nothing to trigger.
     */
    @Override
    public ResponseEntity<Void> importFromOris() {
        return memberDiscoveryJob
                .map(job -> {
                    job.discoverNewMembers();
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> updateMember(
            @PathVariable UUID id,
            UpdateMemberRequest request,
            @ActingUser CurrentUserData currentUser) {

        MemberId memberId = new MemberId(id);
        var prefilled = managementService.prefilledUpdateCommand(memberId);
        var command = UpdateMemberRequestMapper.toCommand(request, prefilled, currentUser.userId());
        Member updatedMember = managementService.updateMember(memberId, command);

        List<String> warnings = updatedMember.birthNumberConsistencyWarnings();
        if (!warnings.isEmpty()) {
            return ResponseEntity.noContent()
                    .header("X-Warnings", warnings.toArray(String[]::new))
                    .build();
        }
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> resumeMember(
            @PathVariable UUID id,
            @ActingUser UserId currentUserId) {

        var command = new Member.ResumeMembership(currentUserId);
        managementService.resumeMember(new MemberId(id), command);
        return ResponseEntity.noContent()
                .location(linkTo(methodOn(MembersApi.class).listMembers(null, null, null, Pageable.unpaged(), null)).toUri())
                .build();
    }

    @Override
    public ResponseEntity<Void> suspendMember(
            @PathVariable UUID id,
            SuspendMembershipRequest request,
            @ActingUser UserId currentUserId) {

        var command = new Member.SuspendMembership(
                currentUserId,
                conversionService.convert(request.reason(), com.klabis.members.domain.DeactivationReason.class),
                request.note()
        );

        managementService.suspendMember(new MemberId(id), command);
        return ResponseEntity.noContent()
                .location(linkTo(methodOn(MembersApi.class).listMembers(null, null, null, Pageable.unpaged(), null)).toUri())
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<List<MemberOptionResponse>> listMemberOptions() {
        List<MemberOptionResponse> options = memberRepository.findAll(MemberFilter.activeOnly()).stream()
                .map(member -> MemberOptionResponseBuilder.builder()
                        .prompt("%s %s (%s)".formatted(member.getFirstName(), member.getLastName(), member.getRegistrationNumber().getValue()))
                        .value(member.getId().uuid().toString())
                        .build())
                .toList();
        return ResponseEntity.ok(options);
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<Page<MemberSummaryResponse>> listMembers(
            @Valid @RequestParam(required = false) String q,
            @Valid @RequestParam(required = false) String status,
            @Valid @RequestParam(required = false) Boolean incomplete,
            @PageableDefault(size = 10, sort = {"lastName", "firstName"}, direction = Sort.Direction.ASC) @ParameterObject Pageable pageable,
            @ActingUser CurrentUserData currentUser) {

        validateSortFields(pageable.getSort());

        MemberFilter filter = buildFilter(q, status, incomplete, currentUser);

        Page<Member> memberPage = memberRepository.findAll(filter, pageable);

        // Same reasoning as getMember: one enrolment lookup per request, scoped to this page's
        // member ids rather than every active MEMBER sync record, read back by the postprocessor
        // via HalResponseContext (design.md D4).
        List<String> pageMemberIds = memberPage.getContent().stream().map(m -> m.getId().uuid().toString()).toList();
        Set<String> enrolledMemberIds = pageMemberIds.isEmpty() || synchronizationPort.isEmpty()
                ? Set.of()
                : synchronizationPort.get().findActiveByTargets(SyncEntityType.MEMBER, pageMemberIds).stream()
                        .map(reference -> reference.target().entityId())
                        .collect(Collectors.toSet());
        HalResponseContext.setContext(new EnrolledMemberIds(enrolledMemberIds));

        HalResponseContext.setContext(new ClubKeyHeld(memberDiscoveryJob.isPresent() && orisClubKeyPort.isSet()));
        HalResponseContext.setDomainList(memberPage.getContent());

        return ResponseEntity.ok(memberPage.map(member -> conversionService.convert(member, MemberSummaryResponse.class)));
    }

    private MemberFilter buildFilter(String q, String status, Boolean incomplete, CurrentUserData currentUser) {
        MemberFilter.StatusFilter resolvedStatus = parseStatus(status);
        boolean canManage = currentUser.hasAuthority(Authority.MEMBERS_MANAGE);

        MemberFilter filter = new MemberFilter(resolvedStatus, q, canManage && Boolean.TRUE.equals(incomplete));

        if (!canManage) {
            filter = filter.withStatus(MemberFilter.StatusFilter.ACTIVE);
        }

        return filter;
    }

    private MemberFilter.StatusFilter parseStatus(String status) {
        if (status == null) {
            return MemberFilter.StatusFilter.ACTIVE;
        }
        try {
            return MemberFilter.StatusFilter.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                    ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                            "Invalid status filter value: " + status +
                            ". Allowed values: ACTIVE, INACTIVE, ALL"
                    ),
                    null);
        }
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("firstName", "lastName", "registrationNumber");

    private void validateSortFields(Sort sort) {
        for (Sort.Order order : sort) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                                "Invalid sort field: " + order.getProperty() +
                                ". Allowed fields: " + ALLOWED_SORT_FIELDS
                        ),
                        null);
            }
        }
    }

    @Override
    public ResponseEntity<MemberDetailsResponse> getMember(
            @PathVariable UUID id,
            @ActingUser CurrentUserData currentUser) {

        MemberId memberId = new MemberId(id);
        Member member = managementService.getMemberAndRecordView(memberId, currentUser.userId(),
                currentUser.hasAuthority(Authority.MEMBERS_MANAGE));

        // Held as Optional like memberDiscoveryJob above: the sync engine is absent from
        // members-only slices (@ApplicationModuleTest without extraIncludes), where the
        // member is simply reported as not enrolled.
        boolean isEnrolled = synchronizationPort
                .map(port -> port.findByTarget(targetFor(memberId)).isPresent())
                .orElse(false);
        Set<String> enrolledIds = isEnrolled ? Set.of(memberId.uuid().toString()) : Set.of();
        HalResponseContext.setContext(new EnrolledMemberIds(enrolledIds));

        Optional<LegalGuardianGroup> guardianGroup = legalGuardianGroupPort.findGroupOf(memberId);
        HalResponseContext.setContext(new MemberLegalGuardianGroup(guardianGroup.orElse(null)));

        HalResponseContext.setDomain(member);
        MemberDetailsResponse response = conversionService.convert(member, MemberDetailsResponse.class);
        return ResponseEntity.ok(MemberDetailsResponseBuilder.builder(response)
                .missingData(liveMissingData(member, guardianGroup))
                .build());
    }

    // The detail is computed live (the guardians' contacts may have changed since the member was last saved),
    // unlike the materialized flag the member list reads.
    private List<MissingDataItem> liveMissingData(Member member, Optional<LegalGuardianGroup> guardianGroup) {
        Set<UserId> guardians = guardianGroup.map(LegalGuardianGroup::getGuardians).orElse(Set.of());
        return memberCompletenessPort.missingData(member, guardians).stream()
                .map(item -> MissingDataItem.valueOf(item.name()))
                .toList();
    }

    private static SyncTarget targetFor(MemberId memberId) {
        return new SyncTarget(SyncEntityType.MEMBER, memberId.uuid().toString());
    }

}

/**
 * Carries which member (by id) is currently paired for synchronisation, from
 * {@code MemberController#getMember} to {@code MemberDetailsPostprocessor} — one lookup per
 * request rather than injecting {@link SynchronizationPort} into the postprocessor, mirroring
 * {@code DisciplineController}'s {@code EnrolledDisciplineIds} (design.md D12).
 */
record EnrolledMemberIds(Set<String> memberIds) {

    boolean contains(UUID memberId) {
        return memberIds.contains(memberId.toString());
    }
}

// Runs before MemberLegalGuardianGroupLinkProcessor, which extends the self link created here.
@MvcComponent
@Order(Ordered.HIGHEST_PRECEDENCE)
class MemberDetailsPostprocessor extends ModelWithDomainPostprocessor<MemberDetailsResponse, Member> {

    private final MemberAccountActivationPort accountActivationPort;

    MemberDetailsPostprocessor(MemberAccountActivationPort accountActivationPort) {
        this.accountActivationPort = accountActivationPort;
    }

    @Override
    public void process(EntityModel<MemberDetailsResponse> dtoModel, Member member) {
        MemberSelfLinkSupport.addSelfLinkWithAffordances(dtoModel, member);

        if (accountActivationPort.isAvailableFor(member)) {
            UUID id = member.getId().uuid();
            dtoModel.mapLink(IanaLinkRelations.SELF, self -> (Link) self.andAffordances(
                    klabisAfford(methodOn(MemberAccountActivationApi.class).sendMemberAccountActivation(id))));
        }

        klabisLinkTo(methodOn(MembersApi.class).listMembers(null, null, null, Pageable.unpaged(), null))
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));

        UUID memberId = member.getId().uuid();
        MemberSelfLinkSupport.addSyncLinkIfEnrolled(dtoModel, memberId);
    }
}

@MvcComponent
class MemberSummaryPostprocessor extends ModelWithDomainPostprocessor<MemberSummaryResponse, Member> {

    @Override
    public void process(EntityModel<MemberSummaryResponse> dtoModel, Member member) {
        MemberSelfLinkSupport.addSelfLinkWithAffordances(dtoModel, member);

        UUID memberId = member.getId().uuid();
        MemberSelfLinkSupport.addSyncLinkIfEnrolled(dtoModel, memberId);
    }
}

/**
 * Self link + status-dependent affordances (suspend/resume) are identical for the member detail
 * and summary DTOs — the logic only touches {@code Member} and {@code RepresentationModel.add},
 * neither of which depends on the DTO's generic type.
 */
final class MemberSelfLinkSupport {

    private MemberSelfLinkSupport() {
    }

    static void addSelfLinkWithAffordances(RepresentationModel<?> dtoModel, Member member) {
        UUID memberId = member.getId().uuid();

        klabisLinkTo(methodOn(MembersApi.class).getMember(memberId, null)).map(link -> {
            var self = link.withSelfRel()
                    .andAffordances(klabisAfford(methodOn(MembersApi.class).updateMember(memberId, null, null)));
            if (member.isActive()) {
                self = self.andAffordances(klabisAfford(
                        methodOn(MembersApi.class).suspendMember(memberId, null, null)));
            } else {
                self = self.andAffordances(klabisAfford(methodOn(MembersApi.class).resumeMember(memberId, null)));
            }
            return (Link) self;
        }).ifPresent(dtoModel::add);
    }

    static void addSyncLinkIfEnrolled(RepresentationModel<?> dtoModel, UUID memberId) {
        if (isEnrolled(memberId)) {
            klabisLinkTo(methodOn(SyncApi.class).getSyncState(SyncEntityTypeParam.MEMBERS, memberId.toString()))
                    .ifPresent(link -> dtoModel.add(link.withRel("sync")));
        }
    }

    private static boolean isEnrolled(UUID memberId) {
        return HalResponseContext.findContext(EnrolledMemberIds.class)
                .map(enrolled -> enrolled.contains(memberId))
                .orElse(false);
    }
}

@MvcComponent
class MemberListPostprocessor implements RepresentationModelProcessor<PagedModel<EntityModel<MemberSummaryResponse>>> {

    @Override
    public PagedModel<EntityModel<MemberSummaryResponse>> process(PagedModel<EntityModel<MemberSummaryResponse>> pagedModel) {
        pagedModel.mapLink(IanaLinkRelations.SELF, selfLink -> (Link) selfLink
                .andAffordances(klabisAfford(methodOn(MembersApi.class).updateMember(null, null, null)))
                .andAffordances(klabisAffordWithOptions(
                        methodOn(RegistrationApi.class).registerMember(null, null),
                        LegalGuardianOptions.forRegistration())));
        return pagedModel;
    }
}

@MvcComponent
class MembersRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(MembersApi.class).listMembers(null, null, null, Pageable.unpaged(), null))
                .ifPresent(link -> model.add(link.withRel("members")));
        return model;
    }
}
