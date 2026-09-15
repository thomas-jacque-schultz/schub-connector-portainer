package schultz.thomas.schub.connector.portainer.business.exceptions;

/** Échec d'un échange avec l'API Portainer. */
public class PortainerException extends RuntimeException {
    public PortainerException(String message) {
        super(message);
    }

    public PortainerException(String message, Throwable cause) {
        super(message, cause);
    }
}
