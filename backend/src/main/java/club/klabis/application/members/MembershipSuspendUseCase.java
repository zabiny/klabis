package club.klabis.application.members;

import club.klabis.domain.members.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class MembershipSuspendUseCase {

    private final MembersRepository membersRepository;

    public MembershipSuspendUseCase(MembersRepository membersRepository) {
        this.membersRepository = membersRepository;
    }

    public Optional<MembershipSuspensionInfo> getSuspensionInfoForMember(Member.Id memberId) {
        return membersRepository.findById(memberId)
                .map(this::suspensionInfoForMember);
    }

    private MembershipSuspensionInfo suspensionInfoForMember(Member member) {
        // TODO: suspension status for finance account...
        return new MembershipSuspensionInfo(member.isSuspended(), MembershipSuspensionInfo.DetailStatus.OK);
    }

    @Transactional
    public void suspendMembershipForMember(Member.Id memberId, boolean forceSuspension) {
        Member memberForSuspension = membersRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        if (forceSuspension || suspensionInfoForMember(memberForSuspension).canSuspendAccount()) {
            memberForSuspension.suspend();
            membersRepository.save(memberForSuspension);
        } else {
            throw new MembershipCannotBeSuspendedException(memberId, "member is already suspended");
        }
    }

}
