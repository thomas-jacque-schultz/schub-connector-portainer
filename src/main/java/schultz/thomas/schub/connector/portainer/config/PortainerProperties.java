package schultz.thomas.schub.connector.portainer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "portainer")
public class PortainerProperties {

    private String baseUrl = "http://admin-portainer:9000";

    private String token = "";

    private Integer endpointId = 2;

    private Duration refreshInterval = Duration.ofSeconds(60);

    private Duration staleAfter = Duration.ofMinutes(5);

    private Duration connectTimeout = Duration.ofSeconds(3);

    private Duration readTimeout = Duration.ofSeconds(10);

    public boolean isConfigured() {
        return token != null && !token.isBlank();
    }
}
