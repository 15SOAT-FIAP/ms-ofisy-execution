package br.com.ofisy.ms_ofisy_execution.domain.exception;

public class InvalidServiceOrderExecutionStatusException extends RuntimeException {

    public InvalidServiceOrderExecutionStatusException(String status) {
        super("Status inválido: " + status);
    }
}
