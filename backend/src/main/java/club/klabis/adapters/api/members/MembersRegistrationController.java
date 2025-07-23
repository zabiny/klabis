package club.klabis.adapters.api.members;

import club.klabis.api.MemberRegistrationsApi;
import club.klabis.api.dto.MemberRegistrationFormApiDto;
import club.klabis.application.MemberRegistrationUseCase;
import club.klabis.domain.members.Member;
import club.klabis.domain.members.forms.RegistrationForm;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class MembersRegistrationController implements MemberRegistrationsApi {

    private final MemberRegistrationUseCase service;
    private final ConversionService conversionService;

    public MembersRegistrationController(MemberRegistrationUseCase service, ConversionService conversionService) {
        this.service = service;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<Void> memberRegistrationsPost(MemberRegistrationFormApiDto memberRegistrationFormApiDto) {
        Member createdMember = service.registerMember(conversionService.convert(memberRegistrationFormApiDto, RegistrationForm.class));
        return ResponseEntity.created(URI.create("/members/%s".formatted(createdMember.getId().value()))).header("MemberId", "%d".formatted(createdMember.getId().value())).build();
    }

}
