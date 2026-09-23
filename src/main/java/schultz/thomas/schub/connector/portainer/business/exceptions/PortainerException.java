package schultz.thomas.schub.connector.portainer.business.exceptions;

public class PortainerException extends RuntimeException {
    public PortainerException(String message) {
        super(message);
    }

    public PortainerException(String message, Throwable cause) {
        super(message, cause);
    }
}
