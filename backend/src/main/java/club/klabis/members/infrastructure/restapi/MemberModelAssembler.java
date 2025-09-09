package club.klabis.members.infrastructure.restapi;

import club.klabis.members.domain.Member;
import club.klabis.members.infrastructure.restapi.dto.MembersApiResponse;
import club.klabis.shared.ConversionService;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class MemberModelAssembler implements RepresentationModelAssembler<Member, EntityModel<MembersApiResponse>> {

    private final ConversionService conversionService;

    public MemberModelAssembler(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public EntityModel<MembersApiResponse> toModel(Member entity) {
        MembersApiResponse responseDto = conversionService.convert(entity, MembersApiResponse.class);

        EntityModel<MembersApiResponse> result = EntityModel.of(responseDto)
                .add(Link.of("/test/1", "example"));

        return result;
    }

    public MembersApiResponse toFullModel(Member entity) {
        MembersApiResponse responseDto = conversionService.convert(entity, MembersApiResponse.class);

        responseDto.add(Link.of("/test/1", "example"));

        return responseDto;
    }
}
