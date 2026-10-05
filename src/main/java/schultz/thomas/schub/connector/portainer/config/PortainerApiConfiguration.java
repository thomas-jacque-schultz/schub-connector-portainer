package schultz.thomas.schub.connector.portainer.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(PortainerProperties.class)
public class PortainerApiConfiguration {

    private final PortainerProperties properties;

    @Bean("portainerRestClient")
    public RestClient portainerRestClient() {
        return build(properties.getReadTimeout());
    }

    @Bean("portainerCommandRestClient")
    public RestClient portainerCommandRestClient() {
        return build(properties.getCommandTimeout());
    }

    private RestClient build(Duration readTimeout) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.getConnectTimeout())
                .withReadTimeout(readTimeout);

        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("X-API-Key", properties.getToken())
                .build();
    }
}
