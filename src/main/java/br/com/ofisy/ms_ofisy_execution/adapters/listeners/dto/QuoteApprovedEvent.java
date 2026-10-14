package br.com.ofisy.ms_ofisy_execution.adapters.listeners.dto;

import br.com.ofisy.ms_ofisy_execution.application.create.CreateServiceOrderExecutionUseCase.CreateServiceOrderExecutionCommand;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuoteApprovedEvent(
        UUID eventId,
        String type,
        UUID serviceOrderId,
        Instant occurredAt,
        Payload payload
) {

    public static final String TYPE = "QuoteApproved";

    public record Payload(UUID quoteId, List<ServiceItem> serviceItems) {}

    public record ServiceItem(UUID serviceCatalogId) {}

    public CreateServiceOrderExecutionCommand toCommand() {
        List<UUID> serviceCatalogIds = payload.serviceItems().stream()
                .map(ServiceItem::serviceCatalogId)
                .toList();
        return new CreateServiceOrderExecutionCommand(eventId, type, serviceOrderId, serviceCatalogIds);
    }
}