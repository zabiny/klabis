package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.authorization.TargetType;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.text.CzechCollation;
import com.klabis.common.users.Authority;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@MvcComponent
class ViewableAccounts {

    record ViewableAccount(UUID memberId, String ownerName) {
    }

    private final AuthorizationEvaluator authorizationEvaluator;
    private final Members members;

    ViewableAccounts(AuthorizationEvaluator authorizationEvaluator, Members members) {
        this.authorizationEvaluator = authorizationEvaluator;
        this.members = members;
    }

    // A single account needs no switcher, and a manager browsing a stranger's account must not see their own
    // family's. Checked on ids first to spare the Members lookup.
    List<ViewableAccount> switcherFor(UUID displayed) {
        Set<UUID> targets = viewableTargets();
        if (targets.size() < 2 || !targets.contains(displayed)) {
            return List.of();
        }
        return inOrder(targets);
    }

    List<ViewableAccount> inOrder() {
        return inOrder(viewableTargets());
    }

    private Set<UUID> viewableTargets() {
        return authorizationEvaluator.targetIdsOf(Authority.FINANCE_VIEW, TargetType.MEMBER);
    }

    // The user's own account comes first so that it is the default landing account; the rest is alphabetical.
    private List<ViewableAccount> inOrder(Set<UUID> targets) {
        if (targets.isEmpty()) {
            return List.of();
        }
        Map<MemberId, MemberDto> owners = members.findByIds(targets.stream().map(MemberId::new).toList());

        return owners.values().stream()
                .sorted(Comparator
                        .comparing((MemberDto m) -> !authorizationEvaluator.isSelf(TargetRef.member(m.memberId())))
                        .thenComparing(MemberDto::lastName, CzechCollation.comparator())
                        .thenComparing(MemberDto::firstName, CzechCollation.comparator()))
                .map(m -> new ViewableAccount(m.memberId(), m.firstName() + " " + m.lastName()))
                .toList();
    }
}
