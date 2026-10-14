package br.com.ofisy.ms_ofisy_execution.adapters.listeners.handlers;

import br.com.ofisy.ms_ofisy_execution.adapters.listeners.EventHandler;
import br.com.ofisy.ms_ofisy_execution.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_execution.adapters.listeners.dto.QuoteApprovedPayload;
import br.com.ofisy.ms_ofisy_execution.application.create.CreateServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.create.CreateServiceOrderExecutionUseCase.CreateServiceOrderExecutionCommand;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuoteApprovedHandler implements EventHandler {

    public static final String TYPE = "QuoteApproved";

    private final CreateServiceOrderExecutionUseCase createServiceOrderExecutionUseCase;
    private final JsonMapper jsonMapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void handle(EventEnvelope envelope) {
        QuoteApprovedPayload payload = jsonMapper.treeToValue(envelope.payload(), QuoteApprovedPayload.class);
        List<UUID> serviceCatalogIds = payload.serviceItems().stream()
                .map(QuoteApprovedPayload.ServiceItem::serviceCatalogId)
                .toList();

        List<ServiceOrderExecution> executions = createServiceOrderExecutionUseCase.execute(
                new CreateServiceOrderExecutionCommand(envelope.eventId(), envelope.type(), envelope.serviceOrderId(), serviceCatalogIds));
        log.info("QuoteApprovedHandler.handle >> event {} processed, {} executions created for service order {}",
                envelope.eventId(), executions.size(), envelope.serviceOrderId());
    }
}