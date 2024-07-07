package club.klabis.adapters.api;

import club.klabis.api.GrantsApi;
import club.klabis.api.dto.GetAllGrants200ResponseApiDto;
import club.klabis.api.dto.GlobalGrantDetailApiDto;
import club.klabis.domain.appusers.ApplicationGrant;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GrantsApiController implements GrantsApi {

    private final ConversionService conversionService;

    public GrantsApiController(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<GetAllGrants200ResponseApiDto> getAllGrants() {
        List<ApplicationGrant> globalGrants = List.of(ApplicationGrant.values());
        List<GlobalGrantDetailApiDto> convertedGrants = (List<GlobalGrantDetailApiDto>) conversionService.convert(globalGrants, TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(GlobalGrantDetailApiDto.class)));
        return ResponseEntity.ok(GetAllGrants200ResponseApiDto.builder().grants(convertedGrants).build());
    }
}
