package club.klabis.groups.domain;

import club.klabis.users.domain.ApplicationGrant;
import club.klabis.groups.domain.forms.EditGroup;
import club.klabis.domain.members.Member;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.util.*;

@AggregateRoot
public class MemberGroup extends AbstractAggregateRoot<MemberGroup> {

    public record Id(int value) {

        private static Id LAST_ID = new Id(0);

        private static Id newId() {
            LAST_ID = new Id(LAST_ID.value() + 1);
            return LAST_ID;
        }
    }

    @Identity
    private final Id id;
    private String name;
    private String description;
    private String email;
    // todo: should be list
    private Member.Id administrator;
    private Set<Member.Id> members = new HashSet<>();
    private Set<GroupPermission> permissions = new HashSet<>();

    public record GroupPermission(ApplicationGrant grant, boolean grantedToOwner, boolean grantedToMembers) {

        public static GroupPermission forOwnerOnly(ApplicationGrant grant) {
            return new GroupPermission(grant, true, false);
        }

        public static GroupPermission forMembersOnly(ApplicationGrant grant) {
            return new GroupPermission(grant, false, true);
        }

        public static GroupPermission forAll(ApplicationGrant grant) {
            return new GroupPermission(grant, true, true);
        }

    }

    public static MemberGroup createGroup(EditGroup createRequest, Member.Id creator) {
        return new MemberGroup(createRequest, creator);
    }

    protected MemberGroup(EditGroup createRequest, Member.Id creator) {
        this.id = Id.newId();
        this.administrator = creator;
        this.name = createRequest.name();
        this.description = createRequest.description();
        this.email = createRequest.email();
        this.members = new HashSet<>(createRequest.members());
        this.permissions = new HashSet<>(createRequest.permissions());
        if (!permissions.isEmpty()) {
            this.andGroupPermissionsChangedForAll();
        }
    }

    public Id getId() {
        return id;
    }

    public Set<Member.Id> getMembers() {
        return new HashSet<>(members);
    }

    public Member.Id getAdministrator() {
        return administrator;
    }

    public String getDescription() {
        return description;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public Collection<GroupPermission> getPermissions() {
        return new HashSet<>(permissions);
    }

    public void update(EditGroup request) {
        final boolean permissionsChanged = !permissions.containsAll(request.permissions()) || !request.permissions().containsAll(
                permissions);

        final Collection<Member.Id> updatedMembers = new HashSet<>();
        if (permissionsChanged) {
            updatedMembers.add(administrator);
            updatedMembers.addAll(members);
            updatedMembers.addAll(request.members());
        } else if (!permissions.isEmpty() || !request.permissions().isEmpty()) {
            Collection<Member.Id> deletedMembers = new HashSet<>(members);
            deletedMembers.removeAll(request.members());

            Collection<Member.Id> addedMembers = new HashSet<>(request.members());
            addedMembers.removeAll(members);

            updatedMembers.addAll(deletedMembers);
            updatedMembers.addAll(addedMembers);
        }

        this.name = request.name();
        this.description = request.description();
        this.email = request.email();
        this.members = new HashSet<>(request.members());
        this.permissions = new HashSet<>(request.permissions());

        andGroupPermissionsChangedForMembersEvents(updatedMembers);
    }

    public void transferOwnership(Member.Id newOwner) {
        Member.Id oldOwner = this.administrator;
        this.administrator = newOwner;

        andGroupPermissionsChangedForMembersEvents(List.of(oldOwner, administrator));
    }

    private void andGroupPermissionsChangedForAll() {
        Collection<Member.Id> allGroupMembers = new ArrayList<>(members);
        allGroupMembers.add(administrator);
        andGroupPermissionsChangedForMembersEvents(allGroupMembers);
    }

    private Collection<ApplicationGrant> getGrantsForMember(Member.Id member) {
        Collection<ApplicationGrant> grantsForMember = new HashSet<>();

        if (member.equals(administrator)) {
            this.permissions.stream()
                    .filter(GroupPermission::grantedToOwner)
                    .map(GroupPermission::grant)
                    .forEach(grantsForMember::add);
        }

        if (members.contains(member) || administrator.equals(member)) {
            this.permissions.stream()
                    .filter(GroupPermission::grantedToMembers)
                    .map(GroupPermission::grant)
                    .forEach(grantsForMember::add);
        }

        return grantsForMember;
    }

    private void andGroupPermissionsChangedForMembersEvents(Collection<Member.Id> changedMembers) {
        changedMembers.stream().map(m -> new GroupMembershipUpdated(m,
                getId(),
                getGrantsForMember(m))).forEach(this::andEvent);
    }
}
