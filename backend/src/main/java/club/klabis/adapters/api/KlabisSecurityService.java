package club.klabis.adapters.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("klabisAuthorizationService")
class KlabisSecurityService {

    private final Logger LOG = LoggerFactory.getLogger(KlabisSecurityService.class);

    public boolean canEditMemberData(int dataMemberId, KlabisUserAuthentication authentication) {
        boolean canEditMemberData = authentication.getPrincipal().getMemberId().map(it -> it.equals(dataMemberId)).orElse(false);

        LOG.trace("Member {} attempt to edit data of member {} - {}", authentication.getPrincipal().getMemberId().orElse(-1), dataMemberId, canEditMemberData);

        return canEditMemberData;
    }

}
