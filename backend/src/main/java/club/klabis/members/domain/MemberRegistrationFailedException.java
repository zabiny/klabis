package club.klabis.members.domain;

public class MemberRegistrationFailedException extends RuntimeException {
    public MemberRegistrationFailedException(String message) {
        super(message);
    }
}
