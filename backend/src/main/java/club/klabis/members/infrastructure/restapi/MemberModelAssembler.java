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

        if (entity.isSuspended()) {
            if (securityService.hasGrant(ApplicationGrant.MEMBERS_SUSPENDMEMBERSHIP)) {
                selfAffordances.add(affordBetter(methodOn(SuspendMemberUseCaseControllers.class).resumeMembership(entity.getId())));
//                target.add(linkTo(methodOn(SuspendMemberUseCaseControllers.class).resumeMembership(entity.getId())).withRel(
//                        ApplicationGrant.MEMBERS_RESUMEMEMBERSHIP.getGrantName()).withName("Obnovit členství"));
            }
        } else {

            if (securityService.canEditMemberData(entity.getId())) {
                selfAffordances.add(affordBetter(methodOn(EditOwnInfoUseCaseControllers.class).membersMemberIdEditOwnMemberInfoFormPut(
                        entity.getId(), null)));
//            target.add(linkTo(methodOn(EditOwnInfoUseCaseControllers.class).membersMemberIdEditOwnMemberInfoFormGet(
//                    entity.getId())).withRel("members:editOwnInfo").withName("Upravit moje údaje"));
            }

            if (securityService.hasGrant(ApplicationGrant.MEMBERS_EDIT)) {
                selfAffordances.add(affordBetter(methodOn(AdminMemberEditUseCaseControllers.class).putMemberEditByAdminForm(
                        entity.getId(), null)));
//            target.add(linkTo(methodOn(AdminMemberEditUseCaseControllers.class).getMemberEditByAdminForm(entity.getId())).withRel(
//                    ApplicationGrant.MEMBERS_EDIT.getGrantName()).withName("Upravit údaje člena klubu"));
            }

            if (securityService.hasGrant(ApplicationGrant.MEMBERS_SUSPENDMEMBERSHIP)) {
                selfAffordances.add(affordBetter(methodOn(SuspendMemberUseCaseControllers.class,
                        entity.getId()).membersMemberIdSuspendMembershipFormPut(
                        entity.getId(), null)));
//            target.add(linkTo(methodOn(SuspendMemberUseCaseControllers.class,
//                    entity.getId()).membersMemberIdSuspendMembershipFormGet(
//                    entity.getId())).withRel(ApplicationGrant.MEMBERS_SUSPENDMEMBERSHIP.getGrantName())
//                    .withName("Pozastavit členství"));
            }

        }
        target.add(entityLinks.linkToItemResource(Member.class, entity.getId().value())
                .withSelfRel()
                .andAffordances(selfAffordances));
    }

    private void removeSelfAffordances(RepresentationModel<?> resourceModel) {
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
