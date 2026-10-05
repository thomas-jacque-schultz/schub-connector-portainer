package schultz.thomas.schub.connector.portainer.api.dto;

import java.time.Instant;

public record Stack(
        Integer id,
        String name,
        Integer endpointId,
        boolean running,
        Instant observedAt,
        CommandFailure lastCommandFailure
) {

    public Stack withLastCommandFailure(CommandFailure failure) {
        return new Stack(id, name, endpointId, running, observedAt, failure);
    }
}
