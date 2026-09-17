package schultz.thomas.schub.connector.portainer.business.services;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import schultz.thomas.schub.connector.portainer.api.dto.Stack;
import schultz.thomas.schub.connector.portainer.config.PortainerProperties;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    /**
     * Fil unique sur lequel partent les commandes vers Portainer.
     *
     * <p>Il existe pour que {@code start} et {@code stop} rendent la main aussitôt : c'est ce
     * qui rend le 202 du contrôleur honnête. Un seul fil, pour que deux commandes reçues à la
     * suite s'appliquent dans cet ordre.</p>
     */
    private final ExecutorService commands =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "portainer-commands");
                t.setDaemon(true);
                return t;
            });

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

    /**
     * Demande le démarrage, et rend la main <strong>immédiatement</strong>.
     *
     * <p>Portainer met des dizaines de secondes à démarrer une stack. Tant que cet appel était
     * synchrone, le 202 renvoyé au-dessus était un mensonge : le cœur attendait, dépassait son
     * délai de lecture de 10 s, et remontait un échec à Discord — « Impossible de lancer le
     * serveur de jeu » — <em>alors que la stack démarrait bel et bien</em>. Deux minutes plus
     * tard, la boucle de réconciliation la voyait passer ONLINE.</p>
     *
     * <p>C'est le §5 du plan appliqué à la lettre : push pour la latence, pull pour la
     * correction. Le connecteur accuse réception, la sonde dit la vérité. Un échec est
     * journalisé et reste invisible de l'appelant — c'est le sens d'un 202 : l'issue n'est pas
     * encore connue.</p>
     *
     * <p>Un seul fil d'exécution, délibérément : deux commandes sur la même stack s'appliquent
     * dans l'ordre où elles ont été reçues.</p>
     */
    public void start(Integer stackId) {
        submit("démarrage", stackId, () -> portainerClient.startStack(stackId));
    }

    /** Symétrique de {@link #start(Integer)} : accepte la demande, ne l'attend pas. */
    public void stop(Integer stackId) {
        submit("arrêt", stackId, () -> portainerClient.stopStack(stackId));
    }

    private void submit(String action, Integer stackId, Runnable call) {
        commands.execute(() -> {
            try {
                call.run();
                log.info("{} de la stack {} transmis à Portainer", action, stackId);
            } catch (RuntimeException e) {
                log.error("{} de la stack {} refusé par Portainer : {}", action, stackId, e.getMessage());
            }
        });
    }

    @PreDestroy
    void stopCommandExecutor() {
        commands.shutdown();
    }
}
