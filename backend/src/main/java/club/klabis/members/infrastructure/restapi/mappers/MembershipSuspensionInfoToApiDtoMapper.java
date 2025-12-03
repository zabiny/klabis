package club.klabis.members.infrastructure.restapi.mappers;

import club.klabis.members.domain.MembershipSuspensionInfo;
import club.klabis.members.infrastructure.restapi.dto.MembershipSuspensionInfoApiDto;
import club.klabis.members.infrastructure.restapi.dto.SuspendMembershipBlockersFinanceApiDto;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
abstract class MembershipSuspensionInfoToApiDtoMapper implements Converter<MembershipSuspensionInfo, MembershipSuspensionInfoApiDto> {

    @Mapping(target = "isSuspended", source = "member.suspended")
    @Mapping(target = "details.finance", source = ".")
    @Mapping(target = "canSuspend", source = ".")
    @Mapping(target = "requestDto.force", constant = "false")
    @Override
    public abstract MembershipSuspensionInfoApiDto convert(MembershipSuspensionInfo membershipSuspensionInfo);

    @Mapping(target = "status", source = "financeAccount")
    abstract SuspendMembershipBlockersFinanceApiDto convertFinanceStatus(MembershipSuspensionInfo source);


    boolean mapOveralStatus(MembershipSuspensionInfo source) {
        return source.canSuspend();
    }

    boolean mapFinanceStatus(MembershipSuspensionInfo.DetailStatus source) {
        return source.canSuspend();
    }
}
