package club.klabis.users.adapters.restapi;

import club.klabis.members.MemberId;
import club.klabis.shared.ApplicationGrant;
import club.klabis.shared.config.security.HasGrant;
import club.klabis.users.adapters.restapi.dto.GetAllGrants200ResponseApiDto;
import club.klabis.users.adapters.restapi.dto.GlobalGrantDetailApiDto;
import club.klabis.users.adapters.restapi.dto.MemberGrantsFormApiDto;
import club.klabis.users.application.ApplicationUserNotFound;
import club.klabis.users.application.ApplicationUsersRepository;
import club.klabis.users.application.UserGrantsUpdateUseCase;
import club.klabis.users.domain.ApplicationUser;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

@RestController
public class UserPermissionsApiController implements UserPermissionsApi {

    private final ConversionService conversionService;
    private final UserGrantsUpdateUseCase userGrantsUpdateUseCase;
    private final ApplicationUsersRepository applicationUsersRepository;

    public UserPermissionsApiController(ConversionService conversionService, UserGrantsUpdateUseCase userGrantsUpdateUseCase, ApplicationUsersRepository applicationUsersRepository) {
        this.conversionService = conversionService;
        this.userGrantsUpdateUseCase = userGrantsUpdateUseCase;
        this.applicationUsersRepository = applicationUsersRepository;
    }

    @Override
    public ResponseEntity<GetAllGrants200ResponseApiDto> getAllGrants() {
        Collection<ApplicationGrant> globalGrants = ApplicationGrant.globalGrants();
        List<GlobalGrantDetailApiDto> convertedGrants = (List<GlobalGrantDetailApiDto>)  conversionService.convert(globalGrants, TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(GlobalGrantDetailApiDto.class)));
        return ResponseEntity.ok(GetAllGrants200ResponseApiDto.builder().grants(convertedGrants).build());
    }


    @HasGrant(ApplicationGrant.APPUSERS_PERMISSIONS)
    @Override
    public ResponseEntity<MemberGrantsFormApiDto> getMemberGrants(Integer memberIdValue) {
        MemberId memberId = new MemberId(memberIdValue);
        ApplicationUser appUser = applicationUsersRepository.findByMemberId(memberId)
                .orElseThrow(() -> ApplicationUserNotFound.forMemberId(memberId));

        return ResponseEntity.ok(conversionService.convert(appUser,
                MemberGrantsFormApiDto.class));
    }

    @HasGrant(ApplicationGrant.APPUSERS_PERMISSIONS)
    @Override
    public ResponseEntity<Void> updateMemberGrants(Integer memberId, MemberGrantsFormApiDto memberGrantsFormApiDto) {
        Collection<ApplicationGrant> globalGrants = (Collection<ApplicationGrant>) conversionService.convert(
                memberGrantsFormApiDto.getGrants(),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(ApplicationGrant.class)));
        userGrantsUpdateUseCase.setGlobalGrants(new MemberId(memberId), globalGrants);
        return ResponseEntity.ok(null);
    }
}
