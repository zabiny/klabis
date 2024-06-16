package club.klabis.domain.members;

public class MemberRegistrationFailedException extends RuntimeException {
    public MemberRegistrationFailedException(String message) {
        super(message);
    }
}
