package schultz.thomas.schub.connector.portainer.config;

import schultz.thomas.schub.connector.portainer.business.services.StackStateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
// Pas de @Scheduled(fixedRateString) : il n'accepte que des ms ou de l'ISO-8601 (PT60S), pas « 60s ».
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
