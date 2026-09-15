package schultz.thomas.schub.connector.portainer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Comment joindre Portainer, et à quel rythme le sonder.
 *
 * <p>L'intervalle est <em>le</em> réglage qui pilote la charge : la sonde lit toutes les stacks
 * en un appel, quel que soit le nombre de serveurs et le nombre d'appelants (plan §6).</p>
 */
@Data
@ConfigurationProperties(prefix = "portainer")
public class PortainerProperties {

    private String baseUrl = "http://admin-portainer:9000";

    /** Jeton d'API. Vide = connecteur en veille, aucune requête émise. */
    private String token = "";

    /**
     * Endpoint Portainer visé par les opérations de démarrage et d'arrêt.
     * Valeur historique du projet : 2.
     */
    private Integer endpointId = 2;

    /** Période de la sonde. Une seule requête Portainer par période, quoi qu'il arrive. */
    private Duration refreshInterval = Duration.ofSeconds(60);

    /**
     * Au-delà de cet âge, une lecture du cache est déclarée périmée et le service se signale
     * comme indisponible plutôt que de servir une valeur en laquelle il ne croit plus.
     */
    private Duration staleAfter = Duration.ofMinutes(5);

    private Duration connectTimeout = Duration.ofSeconds(3);

    private Duration readTimeout = Duration.ofSeconds(10);

    public boolean isConfigured() {
        return token != null && !token.isBlank();
    }
}
