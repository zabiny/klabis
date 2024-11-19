package club.klabis.adapters.api.members.mappers;

import club.klabis.adapters.api.KlabisSecurityService;
import club.klabis.adapters.api.members.MembersController;
import club.klabis.api.dto.MemberViewCompactApiDto;
import club.klabis.common.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.domain.appusers.ApplicationGrant;
import club.klabis.domain.members.Member;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Mapper(config = DomainToDtoMapperConfiguration.class)
public abstract class MemberCompactViewMapper extends RepresentationModelAssemblerSupport<Member, MemberViewCompactApiDto> implements Converter<Member, MemberViewCompactApiDto> {

    private KlabisSecurityService securityService;

    public MemberCompactViewMapper() {
        super(MembersController.class, MemberViewCompactApiDto.class);
    }

    @Mapping(source = "registration", target = "registrationNumber")
    @Override
    abstract public MemberViewCompactApiDto toModel(Member entity);

    @AfterMapping
    void assembleLinks(Member entity, @MappingTarget MemberViewCompactApiDto target) {
        target.add(linkTo(methodOn(MembersController.class).membersMemberIdGet(entity.getId())).withSelfRel());

        if (securityService.canEditMemberData(entity.getId())) {
            target.add(linkTo(methodOn(MembersController.class).membersMemberIdEditOwnMemberInfoFormGet(entity.getId())).withRel("members:editOwnInfo"));
        }

        if (securityService.hasGrant(ApplicationGrant.MEMBERS_EDIT)) {
            target.add(linkTo(methodOn(MembersController.class).getMemberEditByAdminForm(entity.getId())).withRel("members:editAnotherMember"));
        }
    }


    @Override
    public MemberViewCompactApiDto convert(Member source) {
        return toModel(source);
    }

    @Autowired
    public void setSecurityService(KlabisSecurityService securityService) {
        this.securityService = securityService;
    }

    ;
}
