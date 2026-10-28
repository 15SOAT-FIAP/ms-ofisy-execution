package br.com.ofisy.ms_ofisy_execution.application.create;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;

import java.util.List;
import java.util.UUID;

public interface CreateServiceOrderExecutionUseCase {

    List<ServiceOrderExecution> execute(CreateServiceOrderExecutionCommand cmd);

    record CreateServiceOrderExecutionCommand(
            UUID eventId,
            String eventType,
            UUID serviceOrderId,
            List<UUID> serviceCatalogIds
    ) {}
}