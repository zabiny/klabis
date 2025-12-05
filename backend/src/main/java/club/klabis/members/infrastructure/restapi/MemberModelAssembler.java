package club.klabis.members.infrastructure.restapi;

import club.klabis.members.domain.Member;
import club.klabis.members.infrastructure.restapi.dto.MembersApiResponse;
import club.klabis.shared.ConversionService;
import club.klabis.shared.config.hateoas.ModelPreparator;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.KlabisSecurityService;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.affordBetter;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class MemberModelAssembler implements ModelPreparator<Member, MembersApiResponse> {

    private final ConversionService conversionService;
    private final KlabisSecurityService securityService;
    private final EntityLinks entityLinks;

    public MemberModelAssembler(ConversionService conversionService, KlabisSecurityService securityService, EntityLinks entityLinks) {
        this.conversionService = conversionService;
        this.securityService = securityService;
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

    private void removeSelfAffordances(RepresentationModel<?> resourceModel) {
        // TODO: doesn't remove links from RepresentationModelProcessor<EntityModel<?>>. Need to find some way how to do that as that model processors seems to be better place to initialize links (= they can be located at the controller which is referenced there, they are called only when HAL+FORMs data are produced into JSON, etc.. )
        resourceModel.mapLink(LinkRelation.of("self"), Link::withoutAffordances);
    }

    @Override
    public void addLinks(CollectionModel<EntityModel<MembersApiResponse>> model) {
        model.getContent().forEach(this::removeSelfAffordances);

        if (securityService.hasGrant(ApplicationGrant.MEMBERS_REGISTER)) {
            model.mapLink(LinkRelation.of("self"),
                    link -> link.andAffordance(affordBetter(methodOn(RegisterNewMemberController.class).memberRegistrationsPost(
                            null))));
        }
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
