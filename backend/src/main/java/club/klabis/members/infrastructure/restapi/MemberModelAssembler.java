package club.klabis.members.infrastructure.restapi;

import club.klabis.members.domain.Member;
import club.klabis.members.infrastructure.restapi.dto.MemberViewCompactApiDto;
import club.klabis.members.infrastructure.restapi.dto.MembersApiResponse;
import club.klabis.members.infrastructure.restapi.dto.MembersListItemsInnerApiDto;
import club.klabis.shared.ConversionService;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class MemberModelAssembler implements RepresentationModelAssembler<Member, EntityModel<MembersListItemsInnerApiDto>> {

    private final ConversionService conversionService;

    public MemberModelAssembler(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public EntityModel<MembersListItemsInnerApiDto> toModel(Member entity) {
        MembersListItemsInnerApiDto responseDto = conversionService.convert(entity, MemberViewCompactApiDto.class);

        EntityModel<MembersListItemsInnerApiDto> result = EntityModel.of(responseDto)
                .add(Link.of("/test/1", "example"));

        return result;
    }

    public MembersApiResponse toFullModel(Member entity) {
        MembersApiResponse responseDto = conversionService.convert(entity, MembersApiResponse.class);

        responseDto.add(Link.of("/test/1", "example"));

        return responseDto;
    }
}
