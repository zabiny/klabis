package club.klabis.adapters.api.mappers;

import club.klabis.api.dto.MembershipSuspensionInfoApiDto;
import club.klabis.api.dto.SuspendMembershipBlockersFinanceApiDto;
import club.klabis.domain.members.MembershipSuspensionInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToApiDtoMapperConfiguration.class)
interface MembershipSuspensionInfoToApiDtoMapper extends Converter<MembershipSuspensionInfo, MembershipSuspensionInfoApiDto> {

    @Mapping(target = "isSuspended", source = "isMemberSuspended")
    @Mapping(target = "details.finance", source = ".")
    @Mapping(target = "canSuspend", source = ".")
    @Override
    MembershipSuspensionInfoApiDto convert(MembershipSuspensionInfo source);

    @Mapping(target = "status", source = "financeAccountCleared")
    SuspendMembershipBlockersFinanceApiDto convertFinanceStatus(MembershipSuspensionInfo source);

    default boolean mapOverlaStatus(MembershipSuspensionInfo source) {
        return source.canSuspendAccount();
    }
}
