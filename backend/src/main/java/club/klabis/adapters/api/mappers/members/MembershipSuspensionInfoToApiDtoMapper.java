package club.klabis.adapters.api.mappers.members;

import club.klabis.api.dto.MembershipSuspensionInfoApiDto;
import club.klabis.api.dto.SuspendMembershipBlockersFinanceApiDto;
import club.klabis.common.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.domain.members.MembershipSuspensionInfo;
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
