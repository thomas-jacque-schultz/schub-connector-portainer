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

@RestController
@RequestMapping("/stacks")
@RequiredArgsConstructor
public class StackController {

    private final StackStateService stackStateService;

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

    @PostMapping("/refresh")
    public ResponseEntity<List<Stack>> refresh() {
        stackStateService.refresh();
        return list();
    }
}
