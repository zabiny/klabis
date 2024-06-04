package club.klabis.domain.members;

import club.klabis.domain.members.forms.RegistrationForm;
import org.jmolecules.ddd.annotation.Entity;
import org.springframework.data.domain.AbstractAggregateRoot;

// TODO: split into ApplicationUser (with security stuff) and Member (for club Member information)
@Entity
public class Member extends AbstractAggregateRoot<Member> {
    private static int MAX_ID = 0;

    private int id;
    private String firstName;
    private String lastName;

    private RegistrationNumber registration;
    private String password;
    private String googleSubject;
    private String githubSubject;
    private Address address;

    public static Member newMember(RegistrationForm registrationForm) {
        Member result = new Member();
        result.firstName = registrationForm.firstName();
        result.lastName = registrationForm.lastName();
        result.registration = registrationForm.registrationNumber();
        result.address = registrationForm.address();

        return result;
    }

    public static Member newMember(RegistrationNumber registrationNumber, String password) {
        Member result = new Member();
        result.registration = registrationNumber;
        result.password = password;
        return result;
    }

    protected Member() {
        this.password = "{noop}secret";
        this.id = ++MAX_ID;
    }

    public void linkWithGoogle(String googleSubject) {
        this.googleSubject = googleSubject;
    }

    public void linkWithGithub(String githubSubject) {
        this.githubSubject = githubSubject;
    }

    public Address getAddress() {
        return address;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getGithubSubject() {
        return githubSubject;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPassword() {
        return password;
    }

    public RegistrationNumber getRegistration() {
        return registration;
    }

    public int getId() {
        return id;
    }
}
