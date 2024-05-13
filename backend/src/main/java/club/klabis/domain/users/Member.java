package club.klabis.domain.users;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Member {
    private String userName;
    private String password;
    private String googleSubject;
    private String githubSubject;
}
