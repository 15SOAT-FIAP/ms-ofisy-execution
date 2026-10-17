package br.com.ofisy.ms_ofisy_execution.application.start;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;

import java.util.UUID;

public interface StartServiceOrderExecutionUseCase {
    ServiceOrderExecution execute(UUID id);
}