package schultz.thomas.schub.connector.portainer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PortainerConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortainerConnectorApplication.class, args);
    }
}
