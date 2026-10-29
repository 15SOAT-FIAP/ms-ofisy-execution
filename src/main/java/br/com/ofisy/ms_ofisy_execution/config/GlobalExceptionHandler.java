package br.com.ofisy.ms_ofisy_execution.config;

import br.com.ofisy.ms_ofisy_execution.application.exception.ServiceOrderExecutionNotFoundException;
import br.com.ofisy.ms_ofisy_execution.domain.exception.InvalidServiceOrderExecutionStatusException;
import br.com.ofisy.ms_ofisy_execution.domain.exception.InvalidServiceOrderExecutionTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceOrderExecutionNotFoundException.class)
    public ProblemDetail handleServiceOrderExecutionNotFound(ServiceOrderExecutionNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Execução não encontrada");
        return problem;
    }

    @ExceptionHandler(InvalidServiceOrderExecutionStatusException.class)
    public ProblemDetail handleInvalidServiceOrderExecutionStatus(InvalidServiceOrderExecutionStatusException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Status inválido");
        return problem;
    }

    @ExceptionHandler(InvalidServiceOrderExecutionTransitionException.class)
    public ProblemDetail handleInvalidServiceOrderExecutionTransition(InvalidServiceOrderExecutionTransitionException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Transição de status inválida");
        return problem;
    }
}