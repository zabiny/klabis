package club.klabis.events.application;

import club.klabis.members.MemberId;

import java.util.Optional;

public interface MemberSiCardProvider {
    public Optional<String> getSiCardForMember(MemberId memberId);
}
