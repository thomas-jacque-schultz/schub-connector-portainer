package schultz.thomas.schub.connector.portainer.api.dto;

import java.time.Instant;

public record CommandFailure(
        String action,
        String message,
        Instant at
) {
}
