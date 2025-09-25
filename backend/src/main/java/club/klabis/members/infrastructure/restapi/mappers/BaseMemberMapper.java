package club.klabis.members.infrastructure.restapi.mappers;

import club.klabis.members.domain.Member;
import club.klabis.members.infrastructure.restapi.EditMemberUseCaseControllers;
import club.klabis.members.infrastructure.restapi.MembersApi;
import club.klabis.shared.config.hateoas.AbstractRepresentationModelMapper;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.KlabisSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;

import java.util.Collection;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

public abstract class BaseMemberMapper<T extends RepresentationModel<T>> extends AbstractRepresentationModelMapper<Member, T> implements Converter<Member, T> {
    private KlabisSecurityService securityService;

    @Override
    public Collection<Link> createItemLinks(Member entity) {
        Collection<Link> target = super.createItemLinks(entity);

        target.add(linkTo(methodOn(MembersApi.class).membersMemberIdGet(entity.getId().value())).withSelfRel());

        if (entity.isSuspended()) {
            if (securityService.hasGrant(ApplicationGrant.MEMBERS_SUSPENDMEMBERSHIP)) {
                target.add(linkTo(methodOn(EditMemberUseCaseControllers.class).getMemberEditByAdminForm(entity.getId()
                        .value())).withRel(
                        ApplicationGrant.MEMBERS_RESUMEMEMBERSHIP.getGrantName()));
            }
            return target;
        }

        if (securityService.canEditMemberData(entity.getId())) {
            target.add(linkTo(methodOn(EditMemberUseCaseControllers.class).membersMemberIdEditOwnMemberInfoFormGet(
                    entity.getId().value())).withRel(
                    "members:editOwnInfo"));
        }

        if (securityService.hasGrant(ApplicationGrant.MEMBERS_EDIT)) {
            target.add(linkTo(methodOn(EditMemberUseCaseControllers.class).getMemberEditByAdminForm(entity.getId()
                    .value())).withRel(
                    ApplicationGrant.MEMBERS_EDIT.getGrantName()));
        }

        if (securityService.hasGrant(ApplicationGrant.MEMBERS_SUSPENDMEMBERSHIP)) {
            target.add(linkTo(methodOn(EditMemberUseCaseControllers.class).getMemberEditByAdminForm(entity.getId()
                    .value())).withRel(
                    ApplicationGrant.MEMBERS_SUSPENDMEMBERSHIP.getGrantName()));
        }

        if (securityService.hasGrant(ApplicationGrant.APPUSERS_PERMISSIONS)) {
            target.add(linkTo(methodOn(EditMemberUseCaseControllers.class).getMemberEditByAdminForm(entity.getId()
                    .value())).withRel(
                    ApplicationGrant.APPUSERS_PERMISSIONS.getGrantName()));
        }

        return target;
    }

    @Autowired
    public void setSecurityService(KlabisSecurityService securityService) {
        this.securityService = securityService;
    }

    @Override
    public final T convert(Member source) {
        return toModel(source);
    }
}
