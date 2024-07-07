package club.klabis.domain.appusers;

public enum ApplicationGrant {
    MEMBERS_REGISTER("Může registrovat nové členy klubu"),
    MEMBERS_EDIT("Může editovat jiné členy klubu"),
    MEMBERS_SUSPENDMEMBERSHIP("Může suspendovat členství v klubu"),
    APPUSERS_PERMISSIONS("Může spravovat oprávnění v aplikaci");

    private final String description;

    ApplicationGrant(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
