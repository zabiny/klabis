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
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

import java.util.Collection;
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
    public MembersApiResponse mapDataFromDomain(Member member) {
        MembersApiResponse responseDto = conversionService.convert(member, MembersApiResponse.class);

        return responseDto;
    }

    @Override
    public Collection<Link> createCollectionLinks() {
        Collection<Link> result = super.createCollectionLinks();

        if (securityService.hasGrant(ApplicationGrant.MEMBERS_REGISTER)) {
            result.add(linkTo(methodOn(RegisterNewMemberController.class).memberRegistrationsPost(null))
                    .withRel(ApplicationGrant.MEMBERS_REGISTER.getGrantName())
                    .andAffordance(afford(methodOn(RegisterNewMemberController.class).memberRegistrationsPost(null))));
        }

        return result;
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
