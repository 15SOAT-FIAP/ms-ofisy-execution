package br.com.ofisy.ms_ofisy_execution.adapters.presenters;

import br.com.ofisy.ms_ofisy_execution.adapters.controllers.dto.ServiceOrderExecutionResponseDTO;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceOrderExecutionPresenterTest {

    @Test
    void shouldPresentAllFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 17, 9, 0);
        LocalDateTime startedAt = LocalDateTime.of(2026, 10, 17, 10, 0);
        LocalDateTime finishedAt = LocalDateTime.of(2026, 10, 17, 11, 0);
        ServiceOrderExecution execution = ServiceOrderExecution.reconstruct(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                ServiceOrderExecutionStatus.COMPLETED,
                createdAt,
                finishedAt,
                startedAt,
                finishedAt
        );

        ServiceOrderExecutionResponseDTO response = ServiceOrderExecutionPresenter.present(execution);

        assertEquals(execution.getId(), response.id());
        assertEquals(execution.getServiceCatalogId(), response.serviceCatalogId());
        assertEquals(execution.getServiceOrderId(), response.serviceOrderId());
        assertEquals(ServiceOrderExecutionStatus.COMPLETED, response.status());
        assertEquals(createdAt, response.createdAt());
        assertEquals(finishedAt, response.updatedAt());
        assertEquals(startedAt, response.startedAt());
        assertEquals(finishedAt, response.finishedAt());
    }
}