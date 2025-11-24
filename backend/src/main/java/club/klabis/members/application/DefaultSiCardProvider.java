package club.klabis.members.application;

import club.klabis.events.application.MemberSiCardProvider;
import club.klabis.members.MemberId;
import club.klabis.members.domain.Member;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
class DefaultSiCardProvider implements MemberSiCardProvider {
    private final MembersRepository membersRepository;

    public DefaultSiCardProvider(MembersRepository membersRepository) {
        this.membersRepository = membersRepository;
    }

    @Override
    public Optional<String> getSiCardForMember(MemberId memberId) {
        return membersRepository.findById(memberId).flatMap(Member::getSiCard);
    }
}
