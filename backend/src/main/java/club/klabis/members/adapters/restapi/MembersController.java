package club.klabis.members.adapters.restapi;

import club.klabis.members.MemberId;
import club.klabis.members.adapters.restapi.dto.EditMyDetailsFormApiDto;
import club.klabis.members.adapters.restapi.dto.MemberGrantsFormApiDto;
import club.klabis.members.application.EditMemberInfoUseCase;
import club.klabis.members.application.MembersRepository;
import club.klabis.members.application.MembershipSuspendUseCase;
import club.klabis.members.domain.Member;
import club.klabis.members.domain.MemberNotFoundException;
import club.klabis.members.domain.forms.EditAnotherMemberInfoByAdminForm;
import club.klabis.members.domain.forms.EditOwnMemberInfoForm;
import club.klabis.members.domain.forms.MemberEditForm;
import club.klabis.shared.ApplicationGrant;
import club.klabis.shared.config.security.HasGrant;
import club.klabis.users.application.ApplicationUserNotFound;
import club.klabis.users.application.ApplicationUsersRepository;
import club.klabis.users.application.UserGrantsUpdateUseCase;
import club.klabis.users.domain.ApplicationUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

@RestController
public class MembersController implements MembersApi {

    private final MembersRepository membersRepository;
    private final MembershipSuspendUseCase membershipSuspendUseCase;
    private final EditMemberInfoUseCase editMemberUseCase;
    private final ConversionService conversionService;
    private final UserGrantsUpdateUseCase userGrantsUpdateUseCase;
    private final ApplicationUsersRepository applicationUsersRepository;

    public MembersController(MembersRepository membersRepository, MembershipSuspendUseCase membershipSuspendUseCase, EditMemberInfoUseCase editMemberUseCase, ConversionService conversionService, UserGrantsUpdateUseCase userGrantsUpdateUseCase, ApplicationUsersRepository applicationUsersRepository) {
        this.membersRepository = membersRepository;
        this.membershipSuspendUseCase = membershipSuspendUseCase;
        this.editMemberUseCase = editMemberUseCase;
        this.conversionService = conversionService;
        this.userGrantsUpdateUseCase = userGrantsUpdateUseCase;
        this.applicationUsersRepository = applicationUsersRepository;
    }

    @PreAuthorize("@klabisAuthorizationService.canEditMemberData(#memberId)")
    @Override
    public ResponseEntity<club.klabis.members.adapters.restapi.dto.MemberEditFormApiDto> membersMemberIdEditMemberInfoFormGet(Integer memberId) {
        return membersRepository.findById(new MemberId(memberId))
                .map(m -> mapToResponseEntity(m, club.klabis.members.adapters.restapi.dto.MemberEditFormApiDto.class))
                .orElseThrow(() -> new MemberNotFoundException(new MemberId(memberId)));
    }

    @PreAuthorize("@klabisAuthorizationService.canEditMemberData(#memberId)")
    @Override
    public ResponseEntity<Void> membersMemberIdEditMemberInfoFormPut(Integer memberId, club.klabis.members.adapters.restapi.dto.MemberEditFormApiDto memberEditFormApiDto) {
        editMemberUseCase.editMember(new MemberId(memberId),
                conversionService.convert(memberEditFormApiDto, MemberEditForm.class));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Override
    public ResponseEntity<club.klabis.members.adapters.restapi.dto.MemberApiDto> membersMemberIdGet(Integer memberId) {
        return membersRepository.findById(new MemberId(memberId))
                .map(m -> mapToResponseEntity(m, club.klabis.members.adapters.restapi.dto.MemberApiDto.class))
                .orElseThrow(() -> new MemberNotFoundException(new MemberId(memberId)));
    }

    @Override
    public ResponseEntity<club.klabis.members.adapters.restapi.dto.MembersListApiDto> membersGet(String view, Boolean suspended) {
        List<? extends club.klabis.members.adapters.restapi.dto.MembersListItemsInnerApiDto> result = membersRepository.findAll(
                        suspended)
                .stream()
                .map(t -> convertToApiDto(t, view))
                .toList();
        return ResponseEntity.ok(club.klabis.members.adapters.restapi.dto.MembersListApiDto.builder()
                .items((List<club.klabis.members.adapters.restapi.dto.MembersListItemsInnerApiDto>) result)
                .build());
    }

    @Override
    public ResponseEntity<club.klabis.members.adapters.restapi.dto.MembershipSuspensionInfoApiDto> membersMemberIdSuspendMembershipFormGet(Integer memberId) {
        return membershipSuspendUseCase.getSuspensionInfoForMember(new MemberId(memberId))
                .map(d -> mapToResponseEntity(d,
                        club.klabis.members.adapters.restapi.dto.MembershipSuspensionInfoApiDto.class))
                .orElseThrow(() -> new MemberNotFoundException(new MemberId(memberId)));
    }

    @Override
    public ResponseEntity<Void> membersMemberIdSuspendMembershipFormPut(Integer memberId, Boolean force) {
        membershipSuspendUseCase.suspendMembershipForMember(new MemberId(memberId), force);
        return ResponseEntity.ok(null);
    }

    private <T> ResponseEntity<T> mapToResponseEntity(Object data, Class<T> apiDtoType) {
        T payload = conversionService.convert(data, apiDtoType);
        return ResponseEntity.ok(payload);
    }

    private club.klabis.members.adapters.restapi.dto.MembersListItemsInnerApiDto convertToApiDto(Member item, String view) {
        if ("full".equalsIgnoreCase(view)) {
            return conversionService.convert(item, club.klabis.members.adapters.restapi.dto.MemberApiDto.class);
        } else {
            return conversionService.convert(item,
                    club.klabis.members.adapters.restapi.dto.MemberViewCompactApiDto.class);
        }
    }

    @HasGrant(ApplicationGrant.APPUSERS_PERMISSIONS)
    @Override
    public ResponseEntity<MemberGrantsFormApiDto> getMemberGrants(Integer memberIdValue) {
        MemberId memberId = new MemberId(memberIdValue);
        ApplicationUser appUser = applicationUsersRepository.findByMemberId(memberId).orElseThrow(() -> ApplicationUserNotFound.forMemberId(memberId));

        return ResponseEntity.ok(conversionService.convert(appUser,
                club.klabis.members.adapters.restapi.dto.MemberGrantsFormApiDto.class));
    }

    @HasGrant(ApplicationGrant.APPUSERS_PERMISSIONS)
    @Override
    public ResponseEntity<Void> updateMemberGrants(Integer memberId, club.klabis.members.adapters.restapi.dto.MemberGrantsFormApiDto memberGrantsFormApiDto) {
        Collection<ApplicationGrant> globalGrants = (Collection<ApplicationGrant>) conversionService.convert(
                memberGrantsFormApiDto.getGrants(),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(ApplicationGrant.class)));
        userGrantsUpdateUseCase.setGlobalGrants(new MemberId(memberId), globalGrants);
        return ResponseEntity.ok(null);
    }

    /**
     * GET /members/{memberId}/editByAdminForm : Returns data for edit member information form
     * Returns data for edit member information form  #### Required authorization requires &#x60;members:suspendMembership&#x60; grant
     *
     * @param memberId ID of member (required)
     * @return Club member updated successfully (status code 200)
     * or Invalid user input (status code 400)
     * or Missing required user authentication or authentication failed (status code 401)
     * or User is not allowed to perform requested operation (status code 403)
     * or Missing required user authentication or authentication failed (status code 404)
     */
    @Operation(
            operationId = "getMemberEditByAdminForm",
            summary = "Returns data for edit member information form",
            description = "Returns data for edit member information form  #### Required authorization requires `members:suspendMembership` grant",
            tags = {"members", "BFF"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Club member updated successfully", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = club.klabis.members.adapters.restapi.dto.EditAnotherMemberDetailsFormApiDto.class)),
                            @Content(mediaType = "application/problem+json", schema = @Schema(implementation = club.klabis.members.adapters.restapi.dto.EditAnotherMemberDetailsFormApiDto.class))
                    }),
                    @ApiResponse(responseCode = "400", description = "Invalid user input", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = club.klabis.members.adapters.restapi.dto.MembersMemberIdEditMemberInfoFormGet400ResponseApiDto.class)),
                            @Content(mediaType = "application/problem+json", schema = @Schema(implementation = club.klabis.members.adapters.restapi.dto.MembersMemberIdEditMemberInfoFormGet400ResponseApiDto.class))
                    }),
                    @ApiResponse(responseCode = "401", description = "Missing required user authentication or authentication failed", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = club.klabis.shared.RFC7807ErrorResponseApiDto.class)),
                            @Content(mediaType = "application/problem+json", schema = @Schema(implementation = club.klabis.shared.RFC7807ErrorResponseApiDto.class))
                    }),
                    @ApiResponse(responseCode = "403", description = "User is not allowed to perform requested operation", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = club.klabis.members.adapters.restapi.dto.MembersMemberIdEditMemberInfoFormGet403ResponseApiDto.class)),
                            @Content(mediaType = "application/problem+json", schema = @Schema(implementation = club.klabis.members.adapters.restapi.dto.MembersMemberIdEditMemberInfoFormGet403ResponseApiDto.class))
                    }),
                    @ApiResponse(responseCode = "404", description = "Missing required user authentication or authentication failed", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = club.klabis.shared.RFC7807ErrorResponseApiDto.class)),
                            @Content(mediaType = "application/problem+json", schema = @Schema(implementation = club.klabis.shared.RFC7807ErrorResponseApiDto.class))
                    })
            },
            security = {
                    @SecurityRequirement(name = "klabis", scopes = {"openid"})
            }
    )
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/members/{memberId}/editByAdminForm",
            produces = {"application/json", "application/problem+json"}
    )
    @HasGrant(ApplicationGrant.MEMBERS_EDIT)
    @Override
    public ResponseEntity<club.klabis.members.adapters.restapi.dto.EditAnotherMemberDetailsFormApiDto> getMemberEditByAdminForm(Integer memberId) {
        return ResponseEntity.ok(conversionService.convert(editMemberUseCase.getEditAnotherMemberForm(new MemberId(
                memberId)), club.klabis.members.adapters.restapi.dto.EditAnotherMemberDetailsFormApiDto.class));
    }

    @HasGrant(ApplicationGrant.MEMBERS_EDIT)
    @Override
    public ResponseEntity<Void> putMemberEditByAdminForm(Integer memberId, club.klabis.members.adapters.restapi.dto.EditAnotherMemberDetailsFormApiDto editAnotherMemberDetailsFormApiDto) {
        editMemberUseCase.editMember(new MemberId(memberId),
                conversionService.convert(editAnotherMemberDetailsFormApiDto, EditAnotherMemberInfoByAdminForm.class));
        return ResponseEntity.ok(null);
    }

    @Override
    public ResponseEntity<EditMyDetailsFormApiDto> membersMemberIdEditOwnMemberInfoFormGet(Integer memberId) {
        return ResponseEntity.ok(conversionService.convert(editMemberUseCase.getEditOwnMemberInfoForm(new MemberId(
                memberId)), club.klabis.members.adapters.restapi.dto.EditMyDetailsFormApiDto.class));
    }

    @Override
    public ResponseEntity<Void> membersMemberIdEditOwnMemberInfoFormPut(Integer memberId, club.klabis.members.adapters.restapi.dto.EditMyDetailsFormApiDto editMyDetailsFormApiDto) {
        editMemberUseCase.editMember(new MemberId(memberId),
                conversionService.convert(editMyDetailsFormApiDto, EditOwnMemberInfoForm.class));
        return ResponseEntity.ok(null);
    }
}
