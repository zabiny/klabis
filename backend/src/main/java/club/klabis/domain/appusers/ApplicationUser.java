package club.klabis.domain.appusers;

import club.klabis.domain.members.Member;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.util.*;

@AggregateRoot
public class ApplicationUser extends AbstractAggregateRoot<ApplicationUser> {
    private static int MAX_ID = 0;

    @Identity
    private int id;
    private Integer memberId;
    private String username;
    private String password = "{noop}secret";
    private boolean enabled;
    private String googleSubject;
    private String githubSubject;
    private Set<ApplicationGrant> globalGrants = EnumSet.noneOf(ApplicationGrant.class);
    private List<MemberScopedGrant> memberScopedGrants = List.of();

    public static ApplicationUser newAppUser(String username, String password) {
        ApplicationUser result = new ApplicationUser();
        result.id = ++MAX_ID;
        result.username = username;
        result.password = password;
        return result;
    }

    public static ApplicationUser newAppUser(Member member, String password) {
        ApplicationUser result = new ApplicationUser();
        result.id = ++MAX_ID;
        result.username = member.getRegistration().toRegistrationId();
        result.password = password;
        result.memberId = member.getId();
        return result;
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

    public int getId() {
        return id;
    }

    public Optional<Integer> getMemberId() {
        return Optional.ofNullable(memberId);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isEnabled() {
        return enabled;
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