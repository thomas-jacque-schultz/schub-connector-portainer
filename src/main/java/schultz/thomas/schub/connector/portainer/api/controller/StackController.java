package schultz.thomas.schub.connector.portainer.api.controller;

import schultz.thomas.schub.connector.portainer.api.dto.Stack;
import schultz.thomas.schub.connector.portainer.business.services.StackStateService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * API interne du connecteur : les stacks Portainer, et les ordres qui les pilotent.
 *
 * <p>Les lectures viennent d'un cache alimenté par la sonde, jamais de Portainer directement :
 * c'est ce qui rend leur coût nul quel que soit le nombre d'appelants. Chaque stack porte son
 * {@code observedAt} — une lecture trop vieille doit être traitée comme une absence de réponse,
 * pas comme un état.</p>
 *
 * <p>Ce connecteur ne sait pas ce qu'est un serveur de jeu. Il rapporte toutes les stacks, y
 * compris celles d'infrastructure ; c'est à l'appelant de trier.</p>
 */
@RestController
@RequestMapping("/stacks")
@RequiredArgsConstructor
public class StackController {

    private final StackStateService stackStateService;

    /**
     * 503 tant qu'aucune lecture n'a abouti : il n'y a alors aucun état à rapporter, pas même
     * ancien. Répondre une liste vide laisserait croire que Portainer ne gère aucune stack.
     */
    @GetMapping
    public ResponseEntity<List<Stack>> list() {
        return stackStateService.all()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Stack> byId(@PathVariable Integer id) {
        if (stackStateService.all().isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return stackStateService.byId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 202 et non 200 : Portainer bascule le statut de façon asynchrone. Répondre 200 laisserait
     * croire que la stack tourne déjà, alors qu'il faudra une sonde ultérieure pour le savoir.
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<Void> start(@PathVariable Integer id) {
        stackStateService.start(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<Void> stop(@PathVariable Integer id) {
        stackStateService.stop(id);
        return ResponseEntity.accepted().build();
    }

    /** Force une lecture immédiate et rend l'état obtenu. */
    @PostMapping("/refresh")
    public ResponseEntity<List<Stack>> refresh() {
        stackStateService.refresh();
        return list();
    }
}
