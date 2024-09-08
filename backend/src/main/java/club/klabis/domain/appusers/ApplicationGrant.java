package club.klabis.domain.appusers;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum ApplicationGrant {
    MEMBERS_REGISTER("Může registrovat nové členy klubu", true),
    MEMBERS_EDIT("Může editovat jiné členy klubu", true),
    MEMBERS_SUSPENDMEMBERSHIP("Může suspendovat členství v klubu", true),
    APPUSERS_PERMISSIONS("Může spravovat oprávnění v aplikaci", true);

    private final String description;
    // tells if grant represents permission which can be valid across whole application (= can be set to member and then it's valid globally). Use `false` when grant validity is valid only for selected members (must be configured together with scope - either specific member(s) or group(s))
    private final boolean globalGrant;

    ApplicationGrant(String description, boolean globalGrant) {
        this.description = description;
        this.globalGrant = globalGrant;
    }

    public String getDescription() {
        return description;
    }

    private boolean isGlobalGrant() {
        return globalGrant;
    }

    public static Set<ApplicationGrant> globalGrants() {
        return EnumSet.allOf(ApplicationGrant.class).stream().filter(ApplicationGrant::isGlobalGrant).collect(Collectors.toSet());
    }
}
