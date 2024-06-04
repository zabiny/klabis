package club.klabis.domain.members;

public class MemberRegistrationError extends RuntimeException {
    public MemberRegistrationError(String message) {
        super(message);
    }
}
