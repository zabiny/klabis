package club.klabis.members.infrastructure.restapi;

import club.klabis.members.domain.Member;
import club.klabis.members.infrastructure.restapi.dto.MembersApiResponse;
import club.klabis.shared.ConversionService;
import club.klabis.shared.config.hateoas.AbstractRepresentationModelMapper;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.KlabisSecurityService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class MemberModelAssembler extends AbstractRepresentationModelMapper<Member, MembersApiResponse> {

    private final ConversionService conversionService;
    private final KlabisSecurityService securityService;

    public MemberModelAssembler(ConversionService conversionService, KlabisSecurityService securityService) {
        this.conversionService = conversionService;
        this.securityService = securityService;
    }

    @Override
    public MembersApiResponse toResponse(Member member) {
        return conversionService.convert(member, MembersApiResponse.class);
    }


    @Override
    public void addLinks(EntityModel<MembersApiResponse> target) {
        Member entity = target.getContent().getMember();

        target.add(entityLinks.linkToItemResource(Member.class, entity.getId().value()).withSelfRel());

        if (entity.isSuspended()) {
            if (securityService.hasGrant(ApplicationGrant.MEMBERS_SUSPENDMEMBERSHIP)) {
                target.add(linkTo(methodOn(EditMemberUseCaseControllers.class).getMemberEditByAdminForm(entity.getId()
                        .value())).withRel(
                        ApplicationGrant.MEMBERS_RESUMEMEMBERSHIP.getGrantName()));
            }
            return;
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
    }

    @Override
    public void addLinks(CollectionModel<EntityModel<MembersApiResponse>> model) {
        if (securityService.hasGrant(ApplicationGrant.MEMBERS_REGISTER)) {
            model.add(linkTo(methodOn(RegisterNewMemberController.class).memberRegistrationsPost(null))
                    .withRel(ApplicationGrant.MEMBERS_REGISTER.getGrantName())
                    .andAffordance(afford(methodOn(RegisterNewMemberController.class).memberRegistrationsPost(null))));
        }
    }

    String translateDtoToEntityPropertyName(String propertyName) {
        if ("registrationNumber".equals(propertyName)) {
            return "registration";
        } else {
            return propertyName;
        }
    }

    Pageable convertAttributeNamesToEntity(Pageable dtoPageable) {
        List<Sort.Order> updatedSorts = dtoPageable.getSort()
                .stream()
                .map(s -> s.withProperty(translateDtoToEntityPropertyName(s.getProperty())))
                .toList();
        return PageRequest.of(dtoPageable.getPageNumber(), dtoPageable.getPageSize(), Sort.by(updatedSorts));
    }
}
