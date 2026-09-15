package schultz.thomas.schub.connector.portainer.business.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.connector.portainer.config.PortainerProperties;
import schultz.thomas.schub.connector.portainer.model.Stack;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * La sonde d'état : une lecture périodique de Portainer, servie depuis un cache daté.
 *
 * <p>Avant, chaque appelant interrogeait Portainer une fois par serveur et par minute. Ici une
 * seule requête rapporte toutes les stacks, quelle que soit leur nombre <em>et</em> le nombre
 * d'appelants : les lectures deviennent gratuites (plan §6).</p>
 *
 * <p><strong>Un échec de sonde ne rafraîchit pas la date.</strong> Le cache conserve alors sa
 * dernière valeur connue, mais celle-ci vieillit visiblement. C'est délibéré : servir une valeur
 * ancienne avec une date fraîche serait le pire des comportements — l'appelant croirait tenir un
 * état courant. Ici il voit l'âge et décide lui-même.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StackStateService {

    private final PortainerClient portainerClient;

    private final PortainerProperties properties;

    /** Dernière lecture réussie ; vide tant qu'aucune n'a abouti depuis le démarrage. */
    private volatile List<Stack> lastKnown;

    /** Motif du dernier échec, remis à null dès qu'une lecture réussit. */
    private volatile String lastFailure;

    @PostConstruct
    void primeCache() {
        // Sans cette première lecture, le service répondrait « aucun état » pendant toute la
        // première période — soit une minute d'indisponibilité à chaque redémarrage.
        refresh();
    }

    /**
     * Force une lecture immédiate.
     *
     * <p>À appeler après avoir démarré ou arrêté une stack — mais <em>pas</em> dans la foulée
     * immédiate : Portainer bascule son statut de façon asynchrone, et relire aussitôt ne ferait
     * qu'enregistrer l'ancien état avec une date fraîche. C'est précisément le mensonge que
     * {@code observedAt} existe pour empêcher.</p>
     */
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

    /** Vide tant qu'aucune lecture n'a abouti : il n'y a alors aucun état à rapporter, pas même ancien. */
    public Optional<List<Stack>> all() {
        return Optional.ofNullable(lastKnown);
    }

    public Optional<Stack> byId(Integer stackId) {
        return all().flatMap(stacks -> stacks.stream()
                .filter(stack -> stack.id() != null && stack.id().equals(stackId))
                .findFirst());
    }

    /** Âge de la valeur servie, ou vide si aucune lecture n'a abouti. */
    public Optional<Duration> age() {
        return all()
                .filter(stacks -> !stacks.isEmpty())
                .map(stacks -> Duration.between(stacks.get(0).observedAt(), Instant.now()));
    }

    /** Vrai si la dernière lecture réussie est trop ancienne pour être crue. */
    public boolean isStale() {
        return age().map(age -> age.compareTo(properties.getStaleAfter()) > 0).orElse(true);
    }

    public String lastFailure() {
        return lastFailure;
    }

    public void start(Integer stackId) {
        portainerClient.startStack(stackId);
    }

    public void stop(Integer stackId) {
        portainerClient.stopStack(stackId);
    }
}
