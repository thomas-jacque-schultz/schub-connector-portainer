package schultz.thomas.schub.connector.portainer.api.controller;

import schultz.thomas.schub.connector.portainer.business.exceptions.PortainerException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/**
 * 502 plutôt que 500 : la panne est en amont, chez Portainer. L'appelant doit pouvoir
 * distinguer « le connecteur est cassé » de « Portainer ne répond pas » — dans le second cas,
 * sa boucle de réconciliation repassera d'elle-même.
 */
@Slf4j
@RestControllerAdvice
public class PortainerExceptionHandler {

    @ExceptionHandler(PortainerException.class)
    public ProblemDetail handlePortainerFailure(PortainerException exception) {
        log.warn("Échec du dialogue avec Portainer: {}", exception.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
        detail.setTitle("Portainer n'a pas pu être joint ou a refusé l'opération");
        return detail;
    }
}
