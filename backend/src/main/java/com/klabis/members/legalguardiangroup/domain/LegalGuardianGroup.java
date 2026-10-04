package com.klabis.members.legalguardiangroup.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.groups.domain.MemberAlreadyInGroupException;
import com.klabis.common.groups.domain.MemberGroup;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.domain.PersonalInformation;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.text.Collator;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Set of legal guardians shared by minors who have exactly these guardians. Guardians are the owners
 * (users of the system, members or not), minors are the members; a user is never both in one group.
 * Uniqueness of the guardian set and of a minor across groups spans aggregates and is kept by
 * {@code LegalGuardianGroupService}.
 */
@AggregateRoot
public class LegalGuardianGroup extends MemberGroup<LegalGuardianGroup, LegalGuardianGroupId, UserId> {

    public static final String TYPE_DISCRIMINATOR = "LEGAL_GUARDIAN";

    @Identity
    private final LegalGuardianGroupId id;

    private LegalGuardianGroup(LegalGuardianGroupId id, String name, Set<UserId> guardians,
                               Set<GroupMembership<UserId>> minors) {
        super(name, guardians, minors);
        Assert.notNull(id, "LegalGuardianGroupId is required");
        this.id = id;
    }

    /**
     * Guardian is a user of the system and need not be a club member; the last name feeds the generated group name.
     */
    public record Guardian(UserId userId, String lastName) {
        public Guardian {
            Assert.notNull(userId, "Guardian UserId is required");
            Assert.hasText(lastName, "Guardian last name is required");
        }
    }

    public record Minor(MemberId id, LocalDate dateOfBirth) {
        public Minor {
            Assert.notNull(id, "Minor MemberId is required");
            Assert.notNull(dateOfBirth, "Minor date of birth is required");
            if (!PersonalInformation.isMinor(dateOfBirth)) {
                throw new OnlyMinorsAllowedException(id);
            }
        }
    }

    public static LegalGuardianGroup create(Set<Guardian> guardians, Minor minor) {
        Assert.notNull(minor, "Minor is required");
        requireGuardians(guardians);
        requireNoOverlap(guardians, Set.of(minor.id().toUserId()));
        return new LegalGuardianGroup(new LegalGuardianGroupId(UUID.randomUUID()), generateName(guardians),
                toUserIds(guardians), Set.of(GroupMembership.of(minor.id().toUserId())));
    }

    public static LegalGuardianGroup reconstruct(LegalGuardianGroupId id, String name, Set<UserId> guardians,
                                                 Set<GroupMembership<UserId>> minors, AuditMetadata auditMetadata) {
        LegalGuardianGroup group = new LegalGuardianGroup(id, name, guardians, minors);
        group.updateAuditMetadata(auditMetadata);
        return group;
    }

    @Override
    public LegalGuardianGroupId getId() {
        return id;
    }

    @Override
    public Set<Authority> delegatedAuthorities() {
        return Set.of(Authority.MEMBERS_EDIT_PROFILE);
    }

    public Set<UserId> getGuardians() {
        return getOwners();
    }

    public Set<GroupMembership<MemberId>> getMinors() {
        return getMembers().stream()
                .map(m -> new GroupMembership<>(MemberId.fromUserId(m.memberId()), m.joinedAt()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasMinors() {
        return !getMembers().isEmpty();
    }

    public boolean isLastGuardian(UserId userId) {
        return isLastOwner(userId);
    }

    public void changeGuardians(Set<Guardian> guardians) {
        requireGuardians(guardians);
        requireNoOverlap(guardians, getMembers().stream().map(GroupMembership::memberId).collect(Collectors.toSet()));
        Set<UserId> target = toUserIds(guardians);
        target.forEach(this::addOwner);
        // New guardians are added first so the group never passes through an ownerless state
        getOwners().stream().filter(owner -> !target.contains(owner)).toList().forEach(this::removeOwner);
        rename(generateName(guardians));
    }

    public void addMinor(Minor minor) {
        Assert.notNull(minor, "Minor is required");
        UserId minorUserId = minor.id().toUserId();
        // Checked before addMember so the caller keeps the established MemberAlreadyInGroupException (400)
        // carrying the minor's id, instead of the generic owner rejection from MemberGroup.
        if (isOwner(minorUserId)) {
            throw new MemberAlreadyInGroupException(minor.id());
        }
        addMember(minorUserId);
    }

    public void removeMinor(MemberId minor) {
        Assert.notNull(minor, "Minor MemberId is required");
        removeMember(minor.toUserId());
    }

    public void takeMinorsFrom(LegalGuardianGroup other) {
        Assert.notNull(other, "Other group is required");
        other.getMinors().stream().map(GroupMembership::memberId).toList().forEach(minorId -> {
            other.removeMinor(minorId);
            UserId minorUserId = minorId.toUserId();
            if (isOwner(minorUserId)) {
                throw new MemberAlreadyInGroupException(minorId);
            }
            addMember(minorUserId);
        });
    }

    private static void requireGuardians(Set<Guardian> guardians) {
        if (guardians == null || guardians.isEmpty()) {
            throw new LegalGuardianGroupWithoutGuardianException();
        }
    }

    private static void requireNoOverlap(Set<Guardian> guardians, Collection<UserId> minors) {
        guardians.stream()
                .map(Guardian::userId)
                .filter(minors::contains)
                .findFirst()
                .ifPresent(userId -> {
                    throw new MemberAlreadyInGroupException(userId);
                });
    }

    private static Set<UserId> toUserIds(Set<Guardian> guardians) {
        return guardians.stream().map(Guardian::userId).collect(Collectors.toUnmodifiableSet());
    }

    private static String generateName(Set<Guardian> guardians) {
        Collator czech = Collator.getInstance(Locale.forLanguageTag("cs-CZ"));
        List<String> surnames = guardians.stream()
                .map(Guardian::lastName)
                .distinct()
                .sorted(czech)
                .toList();
        return String.join(" a ", surnames);
    }
}
