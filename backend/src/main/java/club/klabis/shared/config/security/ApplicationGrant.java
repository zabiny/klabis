package club.klabis.shared.config.security;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum ApplicationGrant {
    MEMBERS_REGISTER("Může registrovat nové členy klubu", true, false, false, "members:register"),
    MEMBERS_EDIT("Může editovat jiné členy klubu", true, false, false, "members:edit"),
    MEMBERS_SUSPENDMEMBERSHIP("Může suspendovat členství v klubu", true, false, false, "members:suspendMembership"),
    APPUSERS_PERMISSIONS("Může spravovat oprávnění v aplikaci", true, false, false, "members:permissions");

    private final String description;
    // tells if grant represents permission which can be valid across whole application (= can be set to member and then it's valid globally). Use `false` when grant validity is valid only for selected members (must be configured together with scope - either specific member(s) or group(s))
    private final boolean globalGrant;
    // tells if grant can be used as member scoped grant
    private final boolean memberScopedGrant;
    // tells if grant can be used as member group scoped grant
    private final boolean groupScopedGrant;

    private final String grantName;

    ApplicationGrant(String description, boolean globalGrant, boolean memberScopedGrant, boolean groupScopedGrant, String grantName) {
        this.description = description;
        this.globalGrant = globalGrant;
        this.groupScopedGrant = groupScopedGrant;
        this.memberScopedGrant = memberScopedGrant;
        this.grantName = grantName;
    }

    public String getGrantName() {
        return grantName;
    }

    public String getDescription() {
        return description;
    }

    private boolean isGlobalGrant() {
        return globalGrant;
    }

    public boolean isGroupScopedGrant() {
        return groupScopedGrant;
    }

    public boolean isMemberScopedGrant() {
        return memberScopedGrant;
    }

    public static Set<ApplicationGrant> globalGrants() {
        return EnumSet.allOf(ApplicationGrant.class).stream().filter(ApplicationGrant::isGlobalGrant).collect(Collectors.toSet());
    }

    public static ApplicationGrant fromGrantName(String name) {
        return Arrays.stream(ApplicationGrant.values()).filter(it -> name.equals(it.getGrantName())).findFirst().orElseThrow();
    }
}
