package schultz.thomas.schub.connector.portainer.config;

import schultz.thomas.schub.connector.portainer.business.services.StackStateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
/**
 * Programme la sonde d'état à partir de la configuration.
 *
 * <p>Fait ici plutôt qu'avec {@code @Scheduled(fixedRateString = ...)} : cette annotation
 * n'accepte qu'un nombre de millisecondes ou de l'ISO-8601, ce qui obligerait à écrire
 * {@code PT60S} dans la configuration. Passer par le registrar permet de garder
 * {@code refresh-interval: 60s}, lisible par qui exploite le service.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StackProbeScheduling implements SchedulingConfigurer {

    private final StackStateService stackStateService;

    private final PortainerProperties properties;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        log.info("Sonde Portainer programmée toutes les {}s", properties.getRefreshInterval().toSeconds());
        registrar.addFixedRateTask(stackStateService::refresh, properties.getRefreshInterval());
    }
}
