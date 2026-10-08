package br.com.ofisy.ms_ofisy_execution.domain.exception;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;

public class InvalidServiceOrderExecutionTransitionException extends RuntimeException {

    public InvalidServiceOrderExecutionTransitionException(ServiceOrderExecutionStatus from,
                                                           ServiceOrderExecutionStatus to) {
        super("Nao pode alterar o status da execucao de " + from + " para " + to);
    }
}
