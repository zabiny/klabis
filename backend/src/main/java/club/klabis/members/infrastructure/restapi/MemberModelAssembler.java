package club.klabis.members.infrastructure.restapi;

import club.klabis.members.domain.Member;
import club.klabis.members.infrastructure.restapi.dto.MembersApiResponse;
import club.klabis.shared.ConversionService;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.KlabisSecurityService;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;

@Component
public class MemberModelAssembler implements RepresentationModelAssembler<Member, MembersApiResponse> {

    private final ConversionService conversionService;
    private final PagedResourcesAssembler<Member> pagedModelAssembler;
    private final KlabisSecurityService securityService;

    public MemberModelAssembler(ConversionService conversionService, PagedResourcesAssembler<Member> pagedModelAssembler, KlabisSecurityService securityService) {
        this.conversionService = conversionService;
        this.pagedModelAssembler = pagedModelAssembler;
        this.securityService = securityService;
    }

    @Override
    public MembersApiResponse toModel(Member entity) {
        MembersApiResponse responseDto = conversionService.convert(entity, MembersApiResponse.class);

        return responseDto;
    }

    private void addCollectionLinks(RepresentationModel<?> model) {
        if (securityService.hasGrant(ApplicationGrant.MEMBERS_REGISTER)) {
            model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(RegisterNewMemberController.class)
                    .memberRegistrationsPost(null)).withRel(ApplicationGrant.MEMBERS_REGISTER.getGrantName()));
        }

    }

    @Override
    public CollectionModel<MembersApiResponse> toCollectionModel(Iterable<? extends Member> entities) {
        CollectionModel<MembersApiResponse> result = RepresentationModelAssembler.super.toCollectionModel(entities);

        addCollectionLinks(result);

        return result;
    }

    public PagedModel<MembersApiResponse> toPagedModel(Page<Member> entities) {
        PagedModel<MembersApiResponse> result = pagedModelAssembler.toModel(entities, this);

        addCollectionLinks(result);

        return result;
    }

    String translateDtoToEntityPropertyName(String propertyName) {
        if ("registrationNumber".equals(propertyName)) {
            return "registration";
        } else {
            return propertyName;
        }
    }
}
