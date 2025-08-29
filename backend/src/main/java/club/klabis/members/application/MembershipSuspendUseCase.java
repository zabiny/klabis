package club.klabis.members.application;

import club.klabis.members.MemberId;
import club.klabis.members.domain.Member;
import club.klabis.members.domain.MemberNotFoundException;
import club.klabis.members.domain.MembershipCannotBeSuspendedException;
import club.klabis.members.domain.MembershipSuspensionInfo;
import club.klabis.shared.config.ddd.UseCase;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@UseCase
public class MembershipSuspendUseCase {

    private final MembersRepository membersRepository;

    public MembershipSuspendUseCase(MembersRepository membersRepository) {
        this.membersRepository = membersRepository;
    }

    public Optional<MembershipSuspensionInfo> getSuspensionInfoForMember(MemberId memberId) {
        return membersRepository.findById(memberId)
                .map(this::suspensionInfoForMember);
    }

    private MembershipSuspensionInfo suspensionInfoForMember(Member member) {
        // TODO: suspension status for finance account...
        return new MembershipSuspensionInfo(member.isSuspended(), MembershipSuspensionInfo.DetailStatus.OK);
    }

    @Transactional
    public void suspendMembershipForMember(MemberId memberId, boolean forceSuspension) {
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
