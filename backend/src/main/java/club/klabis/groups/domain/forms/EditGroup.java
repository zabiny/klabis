package club.klabis.groups.domain.forms;

import club.klabis.groups.domain.MemberGroup;
import club.klabis.members.MemberId;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public record EditGroup(String name, String description, String email,
                        Collection<MemberGroup.GroupPermission> permissions,
                        Collection<MemberId> members) {

    public EditGroup(String name, String description, String email) {
        this(name, description, email, List.of(), List.of());
    }

    public EditGroup withAddedPermission(MemberGroup.GroupPermission permission) {
        if (permissions.contains(permission)) {
            return this;
        }

        Collection<MemberGroup.GroupPermission> updatedPermissions = new HashSet<>(permissions());
        updatedPermissions.add(permission);
        return withPermissions(updatedPermissions);
    }

    public EditGroup withPermissions(MemberGroup.GroupPermission... permissions) {
        return new EditGroup(name(), description(), email(), List.of(permissions), members());
    }

    public EditGroup withPermissions(Collection<MemberGroup.GroupPermission> permissions) {
        return new EditGroup(name(), description(), email(), permissions, members());
    }

    public EditGroup withAddedMember(MemberId member) {
        return withAddedMembers(List.of(member));
    }

    public EditGroup withAddedMembers(Collection<MemberId> addMembers) {
        if (members.containsAll(addMembers)) {
            return this;
        }

        Collection<MemberId> updatedMembers = new HashSet<>(members());
        updatedMembers.addAll(addMembers);
        return withMembers(updatedMembers);
    }

    public EditGroup withMembers(Collection<MemberId> members) {
        return new EditGroup(name(), description(), email(), permissions(), members);
    }

    public EditGroup withName(String name) {
        return new EditGroup(name, description(), email(), permissions(), members());
    }

    public EditGroup withDescription(String description) {
        return new EditGroup(name(), description, email(), permissions(), members());
    }

    public EditGroup withEmail(String email) {
        return new EditGroup(name(), description(), email, permissions(), members());
    }

}
