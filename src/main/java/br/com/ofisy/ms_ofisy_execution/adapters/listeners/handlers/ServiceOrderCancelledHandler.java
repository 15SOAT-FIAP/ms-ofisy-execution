package br.com.ofisy.ms_ofisy_execution.adapters.listeners.handlers;

import br.com.ofisy.ms_ofisy_execution.adapters.listeners.EventHandler;
import br.com.ofisy.ms_ofisy_execution.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_execution.application.cancelpending.CancelPendingExecutionsUseCase;
import br.com.ofisy.ms_ofisy_execution.application.cancelpending.CancelPendingExecutionsUseCase.CancelPendingExecutionsCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceOrderCancelledHandler implements EventHandler {

    public static final String TYPE = "ServiceOrderCancelled";

    private final CancelPendingExecutionsUseCase cancelPendingExecutionsUseCase;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void handle(EventEnvelope envelope) {
        cancelPendingExecutionsUseCase.execute(
                new CancelPendingExecutionsCommand(envelope.eventId(), envelope.type(), envelope.serviceOrderId()));
        log.info("ServiceOrderCancelledHandler.handle >> event {} processed, executions of service order {} cancelled",
                envelope.eventId(), envelope.serviceOrderId());
    }
}