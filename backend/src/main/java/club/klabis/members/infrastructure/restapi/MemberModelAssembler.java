package club.klabis.members.infrastructure.restapi;

import club.klabis.members.domain.Member;
import club.klabis.members.infrastructure.restapi.dto.MembersApiResponse;
import club.klabis.shared.ConversionService;
import club.klabis.shared.config.hateoas.ModelPreparator;
import org.springframework.hateoas.Affordance;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.affordBetter;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class MemberModelAssembler implements ModelPreparator<Member, MembersApiResponse> {

    private final ConversionService conversionService;
    private final EntityLinks entityLinks;

    public MemberModelAssembler(ConversionService conversionService, EntityLinks entityLinks) {
        this.conversionService = conversionService;
        this.entityLinks = entityLinks;
    }

    @Override
    public MembersApiResponse toResponseDto(Member member) {
        return conversionService.convert(member, MembersApiResponse.class);
    }


    @Override
    public void addLinks(EntityModel<MembersApiResponse> target, Member entity) {
        List<Affordance> selfAffordances = new ArrayList<>();

        target.add(entityLinks.linkToItemResource(Member.class, entity.getId().value())
                .withSelfRel()
                .andAffordances(selfAffordances));
    }

    @Override
    public void addLinks(CollectionModel<EntityModel<MembersApiResponse>> model) {
        model.mapLink(LinkRelation.of("self"),
                link -> link.andAffordances(affordBetter(methodOn(RegisterNewMemberController.class).memberRegistrationsPost(
                        null))));
    }

    @Override
    public String toDomainPropertyName(String dtoPropertyName) {
        if ("registrationNumber".equals(dtoPropertyName)) {
            return "registration";
        } else {
            return dtoPropertyName;
        }
    }
}
