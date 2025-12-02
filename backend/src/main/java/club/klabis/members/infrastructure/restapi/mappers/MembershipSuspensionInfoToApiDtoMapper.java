package club.klabis.members.infrastructure.restapi.mappers;

import club.klabis.members.MemberId;
import club.klabis.members.domain.MembershipSuspensionInfo;
import club.klabis.members.infrastructure.restapi.SuspendMemberUseCaseControllers;
import club.klabis.members.infrastructure.restapi.dto.MembershipSuspensionInfoApiDto;
import club.klabis.members.infrastructure.restapi.dto.SuspendMembershipBlockersFinanceApiDto;
import club.klabis.shared.config.hateoas.ModelPreparator;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.affordBetter;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Mapper(config = DomainToDtoMapperConfiguration.class)
abstract class MembershipSuspensionInfoToApiDtoMapper implements ModelPreparator<MembershipSuspensionInfo, MembershipSuspensionInfoApiDto> {

    @Mapping(target = "isSuspended", source = "member.suspended")
    @Mapping(target = "details.finance", source = ".")
    @Mapping(target = "canSuspend", source = ".")
    @Mapping(target = "requestDto.force", constant = "false")
    @Override
    public abstract MembershipSuspensionInfoApiDto toResponseDto(MembershipSuspensionInfo membershipSuspensionInfo);

    @Mapping(target = "status", source = "financeAccount")
    abstract SuspendMembershipBlockersFinanceApiDto convertFinanceStatus(MembershipSuspensionInfo source);


    boolean mapOveralStatus(MembershipSuspensionInfo source) {
        return source.canSuspend();
    }

    boolean mapFinanceStatus(MembershipSuspensionInfo.DetailStatus source) {
        return source.canSuspend();
    }

    @Override
    public void addLinks(EntityModel<MembershipSuspensionInfoApiDto> resource, MembershipSuspensionInfo membershipSuspensionInfo) {
        MemberId memberId = membershipSuspensionInfo.member().getId();

        resource.add(WebMvcLinkBuilder.linkTo(methodOn(SuspendMemberUseCaseControllers.class).membersMemberIdSuspendMembershipFormGet(
                        memberId)).withSelfRel()
                .andAffordance(affordBetter(methodOn(SuspendMemberUseCaseControllers.class).membersMemberIdSuspendMembershipFormPut(
                        memberId,
                        null))).withSelfRel());
    }
}
