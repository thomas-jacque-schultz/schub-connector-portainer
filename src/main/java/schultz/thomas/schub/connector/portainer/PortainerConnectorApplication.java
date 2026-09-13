package schultz.thomas.schub.connector.portainer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Connecteur Portainer : démarre, arrête et sonde les stacks.
 */
@SpringBootApplication
public class PortainerConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortainerConnectorApplication.class, args);
    }
}
