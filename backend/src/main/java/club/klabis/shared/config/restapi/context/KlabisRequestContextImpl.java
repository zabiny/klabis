package club.klabis.shared.config.restapi.context;

import club.klabis.members.MemberId;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Optional;

@Component
@RequestScope(proxyMode = ScopedProxyMode.INTERFACES)
class KlabisRequestContextImpl implements KlabisRequestContextManager {
    private MemberId memberId;

    public void setMemberId(MemberId memberId) {
        this.memberId = memberId;
    }

    @Override
    public Optional<MemberId> memberIdParam() {
        return Optional.ofNullable(memberId);
    }

}
