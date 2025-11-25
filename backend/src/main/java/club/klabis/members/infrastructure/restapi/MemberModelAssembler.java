package club.klabis.members.infrastructure.restapi;

import club.klabis.members.domain.Member;
import club.klabis.members.infrastructure.restapi.dto.MembersApiResponse;
import club.klabis.shared.ConversionService;
import club.klabis.shared.config.hateoas.ModelPreparator;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.KlabisSecurityService;
import club.klabis.users.infrastructure.restapi.UserPermissionsApi;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class MemberModelAssembler implements ModelPreparator<Member, MembersApiResponse> {

    private final ConversionService conversionService;
    private final KlabisSecurityService securityService;
    private final EntityLinks entityLinks;
    private final LinkRelationProvider linkRelationProvider;

    public MemberModelAssembler(ConversionService conversionService, KlabisSecurityService securityService, EntityLinks entityLinks, LinkRelationProvider linkRelationProvider) {
        this.conversionService = conversionService;
        this.securityService = securityService;
        this.entityLinks = entityLinks;
        this.linkRelationProvider = linkRelationProvider;
    }

    @Override
    public MembersApiResponse toResponseDto(Member member) {
        return conversionService.convert(member, MembersApiResponse.class);
    }


    @Override
    public void addLinks(EntityModel<MembersApiResponse> target, Member entity) {
        target.add(entityLinks.linkToItemResource(Member.class, entity.getId().value()).withSelfRel());
        target.add(entityLinks.linkToCollectionResource(Member.class)
                .withRel(linkRelationProvider.getCollectionResourceRelFor(
                        Member.class)));

        if (entity.isSuspended()) {
            if (securityService.hasGrant(ApplicationGrant.MEMBERS_SUSPENDMEMBERSHIP)) {
                target.add(linkTo(methodOn(SuspendMemberUseCaseControllers.class).resumeMembership(entity.getId())).withRel(
                        ApplicationGrant.MEMBERS_RESUMEMEMBERSHIP.getGrantName()).withName("Obnovit členství"));
            }
            return;
        }

        if (securityService.canEditMemberData(entity.getId())) {
            target.add(linkTo(methodOn(EditOwnInfoUseCaseControllers.class).membersMemberIdEditOwnMemberInfoFormGet(
                    entity.getId())).withRel("members:editOwnInfo").withName("Upravit moje údaje"));
        }

        if (securityService.hasGrant(ApplicationGrant.MEMBERS_EDIT)) {
            target.add(linkTo(methodOn(AdminMemberEditUseCaseControllers.class).getMemberEditByAdminForm(entity.getId())).withRel(
                    ApplicationGrant.MEMBERS_EDIT.getGrantName()).withName("Upravit údaje člena klubu"));
        }

        if (securityService.hasGrant(ApplicationGrant.MEMBERS_SUSPENDMEMBERSHIP)) {
            target.add(linkTo(methodOn(SuspendMemberUseCaseControllers.class,
                    entity.getId()).membersMemberIdSuspendMembershipFormGet(
                    entity.getId())).withRel(ApplicationGrant.MEMBERS_SUSPENDMEMBERSHIP.getGrantName())
                    .withName("Pozastavit členství"));
        }

        if (securityService.hasGrant(ApplicationGrant.APPUSERS_PERMISSIONS)) {
            entity.getAppUserId()
                    .ifPresent(appUserId -> target.add(linkTo(methodOn(UserPermissionsApi.class).getUserGrants(
                            appUserId)).withRel(
                            ApplicationGrant.APPUSERS_PERMISSIONS.getGrantName()).withName("Upravit oprávnění")));
        }
    }

    @Override
    public void addLinks(CollectionModel<EntityModel<MembersApiResponse>> model) {
        if (securityService.hasGrant(ApplicationGrant.MEMBERS_REGISTER)) {
            model.add(linkTo(methodOn(RegisterNewMemberController.class).memberRegistrationsPost(null))
                    .withRel(ApplicationGrant.MEMBERS_REGISTER.getGrantName()).withName("Nový člen klubu"));
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
