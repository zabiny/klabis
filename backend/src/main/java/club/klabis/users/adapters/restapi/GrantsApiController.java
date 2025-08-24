package club.klabis.users.adapters.restapi;

import club.klabis.users.adapters.restapi.dto.GetAllGrants200ResponseApiDto;
import club.klabis.users.adapters.restapi.dto.GlobalGrantDetailApiDto;
import club.klabis.users.domain.ApplicationGrant;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

@RestController
public class GrantsApiController implements GrantsApi {

    private final ConversionService conversionService;

    public GrantsApiController(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<GetAllGrants200ResponseApiDto> getAllGrants() {
        Collection<ApplicationGrant> globalGrants = ApplicationGrant.globalGrants();
        List<GlobalGrantDetailApiDto> convertedGrants = (List<GlobalGrantDetailApiDto>)  conversionService.convert(globalGrants, TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(GlobalGrantDetailApiDto.class)));
        return ResponseEntity.ok(GetAllGrants200ResponseApiDto.builder().grants(convertedGrants).build());
    }
}
