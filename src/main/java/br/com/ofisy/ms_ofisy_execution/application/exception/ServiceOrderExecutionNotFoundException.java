package br.com.ofisy.ms_ofisy_execution.application.exception;

import java.util.UUID;

public class ServiceOrderExecutionNotFoundException extends RuntimeException {

    public ServiceOrderExecutionNotFoundException(UUID id) {
        super("Execução de serviço não encontrada com ID: " + id);
    }
}
