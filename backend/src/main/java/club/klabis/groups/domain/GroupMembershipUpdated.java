package club.klabis.groups.domain;

import club.klabis.members.MemberId;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.domain.DomainEventBase;

import java.util.Collection;

/**
 * Published everytime group either group owners, members and/or permissions changes
 */
public class GroupMembershipUpdated extends DomainEventBase {

    private MemberId member;
    private MemberGroup.Id groupId;
    private Collection<ApplicationGrant> grants;

    protected GroupMembershipUpdated(MemberId member, MemberGroup.Id groupId, Collection<ApplicationGrant> grants) {
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

    public MemberId getMemberId() {
        return member;
    }

}
