package schultz.thomas.schub.connector.portainer.business.services;

import schultz.thomas.schub.connector.portainer.api.dto.Stack;
import schultz.thomas.schub.connector.portainer.business.exceptions.PortainerException;
import schultz.thomas.schub.connector.portainer.config.PortainerProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Les appels bruts à Portainer, et la traduction de ses réponses.
 *
 * <p>Seule classe à connaître les particularités de cette API : clés capitalisées, entiers
 * rendus en {@code Number}, et un {@code Status} numérique où 1 signifie « en marche ».</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortainerClient {

    private static final int STATUS_RUNNING = 1;

    @Qualifier("portainerRestClient")
    private final RestClient restClient;

    private final PortainerProperties properties;

    /** Toutes les stacks en un appel — c'est ce qui rend la sonde indépendante du nombre de serveurs. */
    public List<Stack> listStacks() {
        requireConfigured();
        Instant observedAt = Instant.now();
        try {
            List<Map<String, Object>> raw = restClient.get()
                    .uri("/api/stacks")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (raw == null) {
                return List.of();
            }
            return raw.stream()
                    .map(entry -> toStack(entry, observedAt))
                    .sorted(Comparator.comparing(Stack::name, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .toList();
        } catch (RestClientException e) {
            throw describe("list-stacks", "toutes", e);
        }
    }

    public void startStack(Integer stackId) {
        requireConfigured();
        try {
            restClient.post()
                    .uri("/api/stacks/{id}/start?endpointId={endpoint}", stackId, properties.getEndpointId())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw describe("start-stack", String.valueOf(stackId), e);
        }
    }

    public void stopStack(Integer stackId) {
        requireConfigured();
        try {
            restClient.post()
                    .uri("/api/stacks/{id}/stop?endpointId={endpoint}", stackId, properties.getEndpointId())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw describe("stop-stack", String.valueOf(stackId), e);
        }
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new PortainerException("Aucun jeton Portainer configuré (portainer.token)");
        }
    }

    private Stack toStack(Map<String, Object> raw, Instant observedAt) {
        return new Stack(
                asInt(raw.get("Id")),
                raw.get("Name") != null ? String.valueOf(raw.get("Name")) : null,
                asInt(raw.get("EndpointId")),
                Integer.valueOf(STATUS_RUNNING).equals(asInt(raw.get("Status"))),
                observedAt
        );
    }

    private Integer asInt(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    /**
     * Traduit l'échec en message actionnable.
     *
     * <p>Un 401 dit de vérifier le jeton, un 404 qu'on vise sans doute un endpoint disparu :
     * sans cette distinction, tout ressemble à « Portainer ne marche pas » et le diagnostic
     * recommence de zéro à chaque incident.</p>
     */
    private PortainerException describe(String operation, String target, RestClientException e) {
        if (e instanceof ResourceAccessException) {
            log.error("Portainer injoignable [{} {}] sur {} : {}", operation, target,
                    properties.getBaseUrl(), e.getMessage());
            return new PortainerException("Portainer injoignable: " + e.getMessage(), e);
        }
        if (e instanceof RestClientResponseException response) {
            int status = response.getStatusCode().value();
            if (status == 401 || status == 403) {
                log.error("Portainer refuse l'authentification [{} {}], status={}. Vérifier PORTAINER_TOKEN.",
                        operation, target, status);
                return new PortainerException("Portainer refuse le jeton d'API (" + status + ")", e);
            }
            if (status == 404) {
                log.error("Portainer 404 [{} {}] : stack ou endpoint {} inexistant. Corps: {}",
                        operation, target, properties.getEndpointId(), response.getResponseBodyAsString());
                return new PortainerException("Stack ou endpoint inexistant côté Portainer", e);
            }
            log.error("Portainer en erreur [{} {}], status={}. Corps: {}",
                    operation, target, status, response.getResponseBodyAsString());
            return new PortainerException("Portainer a répondu " + status, e);
        }
        log.error("Échec d'appel Portainer [{} {}] : {}", operation, target, e.getMessage(), e);
        return new PortainerException("Appel Portainer en échec: " + e.getMessage(), e);
    }
}
