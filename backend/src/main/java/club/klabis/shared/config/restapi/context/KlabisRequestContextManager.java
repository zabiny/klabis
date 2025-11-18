package club.klabis.shared.config.restapi.context;

import club.klabis.members.MemberId;

interface KlabisRequestContextManager extends KlabisRequestContext {

    void setMemberId(MemberId memberId);

}
