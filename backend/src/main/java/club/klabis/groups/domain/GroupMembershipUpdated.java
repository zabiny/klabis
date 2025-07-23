package club.klabis.groups.domain;

import club.klabis.domain.DomainEventBase;
import club.klabis.users.domain.ApplicationGrant;
import club.klabis.domain.members.Member;

import java.util.Collection;

/**
 * Published everytime group either group owners, members and/or permissions changes
 */
public class GroupMembershipUpdated extends DomainEventBase {

    private Member.Id member;
    private MemberGroup.Id groupId;
    private Collection<ApplicationGrant> grants;

    protected GroupMembershipUpdated(Member.Id member, MemberGroup.Id groupId, Collection<ApplicationGrant> grants) {
        super();
        this.member = member;
        this.groupId = groupId;
        this.grants = grants;
    }

    public Collection<ApplicationGrant> getGrants() {
        return grants;
    }

    public MemberGroup.Id getGroupId() {
        return groupId;
    }

    public Member.Id getMemberId() {
        return member;
    }

}
