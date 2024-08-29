package club.klabis.domain.members;

public class IncorrectFormDataException extends RuntimeException {
    public IncorrectFormDataException(String message) {
        super(message);
    }
}
