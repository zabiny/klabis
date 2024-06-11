package club.klabis.adapters.api;

import club.klabis.api.MemberRegistrationsApi;
import club.klabis.api.dto.MemberRegistrationFormApiDto;
import club.klabis.domain.members.MemberRegistrationException;
import club.klabis.domain.members.MemberService;
import club.klabis.domain.members.forms.RegistrationForm;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MembersRegistrationController implements MemberRegistrationsApi {

    private final MemberService service;
    private final ConversionService conversionService;

    public MembersRegistrationController(MemberService service, ConversionService conversionService) {
        this.service = service;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<Void> memberRegistrationsPost(MemberRegistrationFormApiDto memberRegistrationFormApiDto) {
        service.registerMember(conversionService.convert(memberRegistrationFormApiDto, RegistrationForm.class));
        return ResponseEntity.ok(null);
    }

}
