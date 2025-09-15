package club.klabis.members.infrastructure.restapi;

import club.klabis.members.domain.Member;
import club.klabis.members.infrastructure.restapi.dto.MembersApiResponse;
import club.klabis.shared.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;

@Component
public class MemberModelAssembler implements RepresentationModelAssembler<Member, MembersApiResponse> {

    private final ConversionService conversionService;
    private final PagedResourcesAssembler<Member> pagedModelAssembler;

    public MemberModelAssembler(ConversionService conversionService, PagedResourcesAssembler<Member> pagedModelAssembler) {
        this.conversionService = conversionService;
        this.pagedModelAssembler = pagedModelAssembler;
    }

    @Override
    public MembersApiResponse toModel(Member entity) {
        MembersApiResponse responseDto = conversionService.convert(entity, MembersApiResponse.class);

        return responseDto;
    }

    @Override
    public CollectionModel<MembersApiResponse> toCollectionModel(Iterable<? extends Member> entities) {
        CollectionModel<MembersApiResponse> result = RepresentationModelAssembler.super.toCollectionModel(entities);

        result.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(RegisterNewMemberController.class)
                .memberRegistrationsPost(null)).withRel("members:register"));

        return result;
    }

    public PagedModel<MembersApiResponse> toPagedModel(Page<Member> entities) {
        PagedModel<MembersApiResponse> result = pagedModelAssembler.toModel(entities, this);

        result.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(RegisterNewMemberController.class)
                .memberRegistrationsPost(null)).withRel("members:register"));

        return result;
    }
}
