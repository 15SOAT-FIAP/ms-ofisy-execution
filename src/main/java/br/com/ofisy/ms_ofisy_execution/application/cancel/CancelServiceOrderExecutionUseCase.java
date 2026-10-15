package br.com.ofisy.ms_ofisy_execution.application.cancel;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;

import java.util.UUID;

public interface CancelServiceOrderExecutionUseCase {
    ServiceOrderExecution execute(UUID id);
}
