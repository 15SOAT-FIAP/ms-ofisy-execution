package br.com.ofisy.ms_ofisy_execution.application.identifybyid;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;

import java.util.UUID;

public interface IdentifyByIdServiceOrderExecutionUseCase {
    ServiceOrderExecution execute(UUID id);
}
