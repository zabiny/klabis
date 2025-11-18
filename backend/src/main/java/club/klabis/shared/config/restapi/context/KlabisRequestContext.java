package club.klabis.shared.config.restapi.context;

import club.klabis.members.MemberId;

import java.util.Optional;

public interface KlabisRequestContext {

    Optional<MemberId> memberIdParam();

}
