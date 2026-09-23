package schultz.thomas.schub.connector.portainer.api.controller;

import schultz.thomas.schub.connector.portainer.business.exceptions.PortainerException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
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
