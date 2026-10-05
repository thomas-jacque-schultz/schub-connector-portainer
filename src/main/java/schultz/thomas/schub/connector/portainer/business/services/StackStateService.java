package schultz.thomas.schub.connector.portainer.business.services;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import schultz.thomas.schub.connector.portainer.api.dto.CommandFailure;
import schultz.thomas.schub.connector.portainer.api.dto.Stack;
import schultz.thomas.schub.connector.portainer.config.PortainerProperties;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class StackStateService {

    private final PortainerClient portainerClient;

    private final PortainerProperties properties;

    private volatile List<Stack> lastKnown;

    private volatile String lastFailure;

    private final Map<Integer, CommandFailure> commandFailures = new ConcurrentHashMap<>();

    // Un seul fil : les commandes sur une même stack s'appliquent dans l'ordre reçu.
    private final ExecutorService commands =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "portainer-commands");
                t.setDaemon(true);
                return t;
            });

    @PostConstruct
    void primeCache() {
        refresh();
    }

    // Ne pas appeler juste après start/stop : Portainer bascule le statut en asynchrone, on daterait l'ancien état.
    public void refresh() {
        if (!properties.isConfigured()) {
            lastFailure = "aucun jeton Portainer configuré (portainer.token)";
            return;
        }
        try {
            lastKnown = portainerClient.listStacks();
            lastFailure = null;
        } catch (RuntimeException e) {
            lastFailure = e.getMessage();
            log.warn("Sonde Portainer en échec, le cache conserve sa dernière valeur et vieillit : {}",
                    e.getMessage());
        }
    }

    public Optional<List<Stack>> all() {
        return Optional.ofNullable(lastKnown)
                .map(stacks -> stacks.stream()
                        .map(stack -> stack.withLastCommandFailure(commandFailures.get(stack.id())))
                        .toList());
    }

    public Optional<Stack> byId(Integer stackId) {
        return all().flatMap(stacks -> stacks.stream()
                .filter(stack -> stack.id() != null && stack.id().equals(stackId))
                .findFirst());
    }

    public Optional<Duration> age() {
        return Optional.ofNullable(lastKnown)
                .filter(stacks -> !stacks.isEmpty())
                .map(stacks -> Duration.between(stacks.get(0).observedAt(), Instant.now()));
    }

    public boolean isStale() {
        return age().map(age -> age.compareTo(properties.getStaleAfter()) > 0).orElse(true);
    }

    public String lastFailure() {
        return lastFailure;
    }

    public void start(Integer stackId) {
        submit("start", stackId, () -> portainerClient.startStack(stackId));
    }

    public void stop(Integer stackId) {
        submit("stop", stackId, () -> portainerClient.stopStack(stackId));
    }

    private void submit(String action, Integer stackId, Runnable call) {
        commands.execute(() -> {
            try {
                call.run();
                commandFailures.remove(stackId);
                log.info("Stack {} : {} transmis à Portainer", stackId, action);
            } catch (RuntimeException e) {
                commandFailures.put(stackId, new CommandFailure(action, e.getMessage(), Instant.now()));
                log.error("Stack {} : {} refusé par Portainer : {}", stackId, action, e.getMessage());
            }
        });
    }

    @PreDestroy
    void stopCommandExecutor() {
        commands.shutdown();
    }
}
