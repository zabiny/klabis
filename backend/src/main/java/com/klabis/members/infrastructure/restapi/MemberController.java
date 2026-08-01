package com.klabis.members.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.ActingUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberFilter;
import com.klabis.members.domain.MemberRepository;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springdoc.core.annotations.ParameterObject;
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
import java.util.Set;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
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
    private final MemberMapper memberMapper;

    public MemberController(
            ManagementPort managementService,
            MemberRepository memberRepository,
            MemberMapper memberMapper) {
        this.managementService = managementService;
        this.memberRepository = memberRepository;
        this.memberMapper = memberMapper;
    }

    @Override
    public ResponseEntity<Void> updateMember(
            @PathVariable UUID id,
            UpdateMemberRequest request,
            @ActingUser CurrentUserData currentUser) {

        MemberId memberId = new MemberId(id);
        var command = UpdateMemberRequestMapper.toCommand(request, currentUser.userId());
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
                .location(linkTo(methodOn(MembersApi.class).listMembers(null, null, Pageable.unpaged(), null)).toUri())
                .build();
    }

    @Override
    public ResponseEntity<Void> suspendMember(
            @PathVariable UUID id,
            SuspendMembershipRequest request,
            @ActingUser UserId currentUserId) {

        var command = new Member.SuspendMembership(
                currentUserId,
                request.reason(),
                request.note()
        );

        managementService.suspendMember(new MemberId(id), command);
        return ResponseEntity.noContent()
                .location(linkTo(methodOn(MembersApi.class).listMembers(null, null, Pageable.unpaged(), null)).toUri())
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<List<MemberOptionResponse>> listMemberOptions() {
        List<MemberOptionResponse> options = memberRepository.findAll(MemberFilter.activeOnly()).stream()
                .map(member -> new MemberOptionResponse(
                        member.getId().uuid().toString(),
                        "%s %s (%s)".formatted(member.getFirstName(), member.getLastName(), member.getRegistrationNumber().getValue())
                ))
                .toList();
        return ResponseEntity.ok(options);
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<Page<MemberSummaryResponse>> listMembers(
            @Valid @RequestParam(required = false) String q,
            @Valid @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = {"lastName", "firstName"}, direction = Sort.Direction.ASC) @ParameterObject Pageable pageable,
            @ActingUser CurrentUserData currentUser) {

        validateSortFields(pageable.getSort());

        MemberFilter filter = buildFilter(q, status, currentUser);

        Page<Member> memberPage = memberRepository.findAll(filter, pageable);

        HalResponseContext.setDomainList(memberPage.getContent());

        return ResponseEntity.ok(memberPage.map(memberMapper::toSummaryResponse));
    }

    private MemberFilter buildFilter(String q, String status, CurrentUserData currentUser) {
        MemberFilter.StatusFilter resolvedStatus = parseStatus(status);

        MemberFilter filter = new MemberFilter(resolvedStatus, q);

        if (!currentUser.hasAuthority(Authority.MEMBERS_MANAGE)) {
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

        HalResponseContext.setDomain(member);
        return ResponseEntity.ok(memberMapper.toDetailsResponse(member));
    }

}

@MvcComponent
class MemberDetailsPostprocessor extends ModelWithDomainPostprocessor<MemberDetailsResponse, Member> {

    @Override
    public void process(EntityModel<MemberDetailsResponse> dtoModel, Member member) {
        MemberSelfLinkSupport.addSelfLinkWithAffordances(dtoModel, member);

        klabisLinkTo(methodOn(MembersApi.class).listMembers(null, null, Pageable.unpaged(), null))
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));
    }
}

@MvcComponent
class MemberSummaryPostprocessor extends ModelWithDomainPostprocessor<MemberSummaryResponse, Member> {

    @Override
    public void process(EntityModel<MemberSummaryResponse> dtoModel, Member member) {
        MemberSelfLinkSupport.addSelfLinkWithAffordances(dtoModel, member);
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
                self = self.andAffordances(klabisAfford(methodOn(MembersApi.class).suspendMember(memberId, null, null)));
            } else {
                self = self.andAffordances(klabisAfford(methodOn(MembersApi.class).resumeMember(memberId, null)));
            }
            return (Link) self;
        }).ifPresent(dtoModel::add);
    }
}

@MvcComponent
class MemberListPostprocessor implements RepresentationModelProcessor<PagedModel<EntityModel<MemberSummaryResponse>>> {

    @Override
    public PagedModel<EntityModel<MemberSummaryResponse>> process(PagedModel<EntityModel<MemberSummaryResponse>> pagedModel) {
        pagedModel.mapLink(IanaLinkRelations.SELF, selfLink -> (Link) selfLink
                .andAffordances(klabisAfford(methodOn(MembersApi.class).updateMember(null, null, null)))
                .andAffordances(klabisAfford(methodOn(RegistrationApi.class).registerMember(null, null))));
        return pagedModel;
    }
}

@MvcComponent
class MembersRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(MembersApi.class).listMembers(null, null, Pageable.unpaged(), null))
                .ifPresent(link -> model.add(link.withRel("members")));
        return model;
    }
}
