package club.klabis.domain.memberGroups.forms;

import club.klabis.domain.memberGroups.MemberGroup;
import club.klabis.domain.members.Member;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public record EditGroup(String name, String description, String email,
                        Collection<MemberGroup.GroupPermission> permissions,
                        Collection<Member.Id> members) {

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

    public EditGroup withAddedMember(Member.Id member) {
        return withAddedMembers(List.of(member));
    }

    public EditGroup withAddedMembers(Collection<Member.Id> addMembers) {
        if (members.containsAll(addMembers)) {
            return this;
        }

        Collection<Member.Id> updatedMembers = new HashSet<>(members());
        updatedMembers.addAll(addMembers);
        return withMembers(updatedMembers);
    }

    public EditGroup withMembers(Collection<Member.Id> members) {
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
