package br.com.ofisy.ms_ofisy_execution.adapters.controllers.dto;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceOrderExecutionResponseDTO(
        UUID id,
        UUID serviceCatalogId,
        UUID serviceOrderId,
        ServiceOrderExecutionStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {}