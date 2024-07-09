package club.klabis.domain.members;

public class IncorrectRegistrationDataException extends RuntimeException {
    public IncorrectRegistrationDataException(String message) {
        super(message);
    }
}
