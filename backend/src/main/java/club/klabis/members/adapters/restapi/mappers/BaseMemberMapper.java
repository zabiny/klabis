package club.klabis.members.adapters.restapi.mappers;

import club.klabis.config.security.KlabisSecurityService;
import club.klabis.members.adapters.restapi.MembersController;
import club.klabis.users.domain.ApplicationGrant;
import club.klabis.members.domain.Member;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

public abstract class BaseMemberMapper<T extends RepresentationModel<T>> extends RepresentationModelAssemblerSupport<Member, T> implements Converter<Member, T> {
    private KlabisSecurityService securityService;

    public BaseMemberMapper(Class<?> controllerClass, Class<T> resourceType) {
        super(controllerClass, resourceType);
    }

    @AfterMapping
    void assembleLinks(Member entity, @MappingTarget T target) {
        target.add(linkTo(methodOn(MembersController.class).membersMemberIdGet(entity.getId().value())).withSelfRel());

        if (securityService.canEditMemberData(entity.getId().value())) {
            target.add(linkTo(methodOn(MembersController.class).membersMemberIdEditOwnMemberInfoFormGet(entity.getId().value())).withRel(
                    "members:editOwnInfo"));
        }

        if (securityService.hasGrant(ApplicationGrant.MEMBERS_EDIT)) {
            target.add(linkTo(methodOn(MembersController.class).getMemberEditByAdminForm(entity.getId().value())).withRel(
                    "members:editAnotherMember"));
        }
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
