package com.klabis.usergroups.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import org.springframework.util.Assert;

import java.util.Set;
import java.util.UUID;

public class FamilyGroup extends UserGroup {

    public static final String TYPE_DISCRIMINATOR = "FAMILY";

    private FamilyGroup(UserGroupId id, String name, Set<MemberId> owners, Set<GroupMembership> members) {
        super(id, name, owners, members);
    }

    @Override
    public String typeDiscriminator() {
        return TYPE_DISCRIMINATOR;
    }

    public record CreateFamilyGroup(String name, MemberId owner, Set<MemberId> initialMembers) {
        public CreateFamilyGroup {
            Assert.hasText(name, "Group name is required");
            Assert.notNull(owner, "Owner is required");
            Assert.notNull(initialMembers, "Initial members set is required");
        }
    }

    public static FamilyGroup create(CreateFamilyGroup command) {
        UserGroupId id = new UserGroupId(UUID.randomUUID());
        FamilyGroup group = new FamilyGroup(id, command.name(), Set.of(command.owner()), Set.of());
        command.initialMembers().forEach(group::addMember);
        return group;
    }

    public static FamilyGroup reconstruct(UserGroupId id, String name, Set<MemberId> owners,
                                          Set<GroupMembership> members, AuditMetadata auditMetadata) {
        FamilyGroup group = new FamilyGroup(id, name, owners, members);
        group.updateAuditMetadata(auditMetadata);
        return group;
    }
}
