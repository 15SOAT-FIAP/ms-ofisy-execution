package br.com.ofisy.ms_ofisy_execution.adapters.listeners;

import br.com.ofisy.ms_ofisy_execution.adapters.listeners.dto.EventEnvelope;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ExecutionEventsListener {

    private final Map<String, EventHandler> handlers;

    public ExecutionEventsListener(List<EventHandler> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(EventHandler::type, Function.identity()));
    }

    @SqsListener("${ofisy.messaging.queues.execution}")
    public void onMessage(EventEnvelope envelope) {
        EventHandler handler = handlers.get(envelope.type());
        if (handler == null) {
            log.warn("ExecutionEventsListener.onMessage >> ignoring event {} of type {}", envelope.eventId(), envelope.type());
            return;
        }

        handler.handle(envelope);
    }
}