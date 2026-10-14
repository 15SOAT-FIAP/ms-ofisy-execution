package br.com.ofisy.ms_ofisy_execution.adapters.listeners;

import br.com.ofisy.ms_ofisy_execution.adapters.listeners.dto.QuoteApprovedEvent;
import br.com.ofisy.ms_ofisy_execution.application.create.CreateServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuoteApprovedListener {

    private final CreateServiceOrderExecutionUseCase createServiceOrderExecutionUseCase;

    @SqsListener("${ofisy.messaging.queues.execution}")
    public void onMessage(QuoteApprovedEvent event) {
        if (!QuoteApprovedEvent.TYPE.equals(event.type())) {
            log.warn("QuoteApprovedListener.onMessage >> ignoring event {} of type {}", event.eventId(), event.type());
            return;
        }

        List<ServiceOrderExecution> executions = createServiceOrderExecutionUseCase.execute(event.toCommand());
        log.info("QuoteApprovedListener.onMessage >> event {} processed, {} executions created for service order {}",
                event.eventId(), executions.size(), event.serviceOrderId());
    }
}