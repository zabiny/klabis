package club.klabis.common.domain;

public class IncorrectFormDataException extends RuntimeException {
    public IncorrectFormDataException(String message) {
        super(message);
    }
}
