package club.klabis.domain;

public class IncorrectFormDataException extends RuntimeException {
    public IncorrectFormDataException(String message) {
        super(message);
    }
}
