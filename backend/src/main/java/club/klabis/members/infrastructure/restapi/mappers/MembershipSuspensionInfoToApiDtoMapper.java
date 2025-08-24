package club.klabis.members.infrastructure.restapi.mappers;

import club.klabis.members.domain.MembershipSuspensionInfo;
import club.klabis.members.infrastructure.restapi.dto.MembershipSuspensionInfoApiDto;
import club.klabis.members.infrastructure.restapi.dto.SuspendMembershipBlockersFinanceApiDto;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface MembershipSuspensionInfoToApiDtoMapper extends Converter<MembershipSuspensionInfo, MembershipSuspensionInfoApiDto> {

    @Mapping(target = "isSuspended", source = "isMemberSuspended")
    @Mapping(target = "details.finance", source = ".")
    @Mapping(target = "canSuspend", source = ".")
    @Override
    MembershipSuspensionInfoApiDto convert(MembershipSuspensionInfo source);

    @Mapping(target = "status", source = "financeAccount")
    SuspendMembershipBlockersFinanceApiDto convertFinanceStatus(MembershipSuspensionInfo source);

    default boolean mapOveralStatus(MembershipSuspensionInfo source) {
        return source.canSuspendAccount();
    }
    default boolean mapFinanceStatus(MembershipSuspensionInfo.DetailStatus source) {
        return source.canSuspend();
    }
}
