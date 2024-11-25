package club.klabis.domain.appusers;

import club.klabis.domain.members.Member;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.data.domain.AbstractAggregateRoot;

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
    private Member.Id memberId;
    private String userName;
    private String password = "{noop}secret";
    private boolean enabled = true;
    private String googleSubject;
    private String githubSubject;
    private Set<ApplicationGrant> globalGrants = EnumSet.noneOf(ApplicationGrant.class);
    private List<MemberScopedGrant> memberScopedGrants = List.of();

    public static ApplicationUser newAppUser(String username, String password) {
        ApplicationUser result = new ApplicationUser();
        result.userName = username;
        result.password = password;
        return result;
    }

    public static ApplicationUser newAppUser(Member member, String password) {
        ApplicationUser result = new ApplicationUser();
        result.userName = member.getRegistration().toRegistrationId();
        result.password = password;
        result.memberId = member.getId();
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

    public Optional<Member.Id> getMemberId() {
        return Optional.ofNullable(memberId);
    }

    public String getUsername() {
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
}

record MemberScopedGrant(ApplicationGrant grant, Integer scopedMemberId) {

}