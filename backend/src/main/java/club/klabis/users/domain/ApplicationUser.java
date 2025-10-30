package club.klabis.users.domain;

import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.users.domain.events.ApplicationUserEnableStatusChanged;
import io.micrometer.common.util.StringUtils;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.util.Assert;

import java.util.*;

@AggregateRoot
public class ApplicationUser extends AbstractAggregateRoot<ApplicationUser> {
    public record Id(int value) {

        private static Id LAST_ID = new Id(0);

        private static Id newId() {
            LAST_ID = new Id(LAST_ID.value() + 1);
            return LAST_ID;
        }
    }

    @Identity
    private final Id id;
    private UserName userName;
    private String password = "{noop}secret";
    private boolean enabled = true;
    private String googleSubject;
    private String githubSubject;
    private Set<ApplicationGrant> globalGrants = EnumSet.noneOf(ApplicationGrant.class);
    private List<UserScopedGrant> userScopedGrants = List.of();

    public record UserName(String value) {
        public UserName {
            Assert.notNull(value, "UserName must not be null");
            Assert.isTrue(StringUtils.isNotBlank(value), "UserName must not be blank");
        }

        public static UserName of(String value) {
            return new UserName(value);
        }

    }

    public static ApplicationUser newAppUser(UserName username, String password) {
        ApplicationUser result = new ApplicationUser();
        result.userName = username;
        result.password = password;
        return result;
    }

    public ApplicationUser() {
        this.id = Id.newId();
    }

    public void linkWithGoogle(String googleSubject) {
        this.googleSubject = googleSubject;
    }

    public void linkWithGithub(String githubSubject) {
        this.githubSubject = githubSubject;
    }


    public Optional<String> getGithubSubject() {
        return Optional.ofNullable(githubSubject);
    }

    public Optional<String> getGoogleSubject() {
        return Optional.ofNullable(googleSubject);
    }

    public Id getId() {
        return id;
    }

    public UserName getUsername() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public boolean isDisabled() {
        return !enabled;
    }

    public Set<ApplicationGrant> getGlobalGrants() {
        return globalGrants;
    }

    public void setGlobalGrants(Collection<ApplicationGrant> globalGrants) {
        this.globalGrants.clear();
        this.globalGrants.addAll(globalGrants);
    }

    public void disable() {
        if (this.enabled) {
            this.enabled = false;
            this.andEvent(new ApplicationUserEnableStatusChanged(this));
        }
    }

    public void enable() {
        if (!this.enabled) {
            this.enabled = true;
            this.andEvent(new ApplicationUserEnableStatusChanged(this));
        }
    }
}

record UserScopedGrant(ApplicationGrant grant, Integer scopedMemberId) {

}