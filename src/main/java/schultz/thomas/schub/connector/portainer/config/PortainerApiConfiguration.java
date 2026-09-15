package schultz.thomas.schub.connector.portainer.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Client HTTP vers l'API Portainer, authentifié par le jeton d'API. */
@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(PortainerProperties.class)
public class PortainerApiConfiguration {

    private final PortainerProperties properties;

    @Bean("portainerRestClient")
    public RestClient portainerRestClient() {
        // Sans bornes explicites, un Portainer qui ne répond plus ferait pendre la sonde et,
        // avec elle, toute opération de démarrage déclenchée pendant ce temps.
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.getConnectTimeout())
                .withReadTimeout(properties.getReadTimeout());

        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("X-API-Key", properties.getToken())
                .build();
    }
}
