package club.klabis.shared.config.ddd;

public class InvalidFormDataException extends Exception {
    public InvalidFormDataException(String message) {
        super(message);
    }

    public InvalidFormDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
