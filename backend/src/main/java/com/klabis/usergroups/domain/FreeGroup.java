package com.klabis.usergroups.domain;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.springframework.util.Assert;

import java.util.Set;
import java.util.UUID;

public class FreeGroup extends UserGroup {

    public static final String TYPE_DISCRIMINATOR = "FREE";

    private FreeGroup(UserGroupId id, String name, Set<MemberId> owners, Set<GroupMembership> members) {
        super(id, name, owners, members);
    }

    @RecordBuilder
    public record CreateFreeGroup(String name, MemberId creator) {
        public CreateFreeGroup {
            Assert.hasText(name, "Group name is required");
            Assert.notNull(creator, "Creator is required");
        }
    }

    public static FreeGroup create(CreateFreeGroup command) {
        UserGroupId id = new UserGroupId(UUID.randomUUID());
        GroupMembership creatorMembership = GroupMembership.of(command.creator());
        return new FreeGroup(id, command.name(), Set.of(command.creator()), Set.of(creatorMembership));
    }

    public static FreeGroup reconstruct(UserGroupId id, String name, Set<MemberId> owners,
                                        Set<GroupMembership> members, com.klabis.common.domain.AuditMetadata auditMetadata) {
        FreeGroup group = new FreeGroup(id, name, owners, members);
        group.updateAuditMetadata(auditMetadata);
        return group;
    }
}
