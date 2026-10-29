package br.com.ofisy.ms_ofisy_execution.application.complete;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;

import java.util.UUID;

public interface CompleteServiceOrderExecutionUseCase {
    ServiceOrderExecution execute(UUID id);
}