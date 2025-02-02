package club.klabis.adapters.api.members.mappers;

import club.klabis.adapters.api.KlabisSecurityService;
import club.klabis.adapters.api.members.MembersController;
import club.klabis.api.dto.MemberApiDto;
import club.klabis.domain.appusers.ApplicationGrant;
import club.klabis.domain.members.Member;
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
