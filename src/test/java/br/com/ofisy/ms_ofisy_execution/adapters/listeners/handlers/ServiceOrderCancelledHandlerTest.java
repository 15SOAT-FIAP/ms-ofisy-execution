package br.com.ofisy.ms_ofisy_execution.adapters.listeners.handlers;

import br.com.ofisy.ms_ofisy_execution.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_execution.application.cancelpending.CancelPendingExecutionsUseCase;
import br.com.ofisy.ms_ofisy_execution.application.cancelpending.CancelPendingExecutionsUseCase.CancelPendingExecutionsCommand;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ServiceOrderCancelledHandlerTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID SERVICE_ORDER_ID = UUID.randomUUID();

    @Mock
    private CancelPendingExecutionsUseCase cancelPendingExecutionsUseCase;

    @Captor
    private ArgumentCaptor<CancelPendingExecutionsCommand> commandCaptor;

    @InjectMocks
    private ServiceOrderCancelledHandler handler;

    @Test
    void shouldHandleServiceOrderCancelledType() {
        assertThat(handler.type()).isEqualTo("ServiceOrderCancelled");
    }

    @Nested
    class Handle {

        @Test
        void shouldMapEnvelopeToCancelPendingCommand() {
            EventEnvelope envelope = new EventEnvelope(EVENT_ID, ServiceOrderCancelledHandler.TYPE, SERVICE_ORDER_ID,
                    Instant.parse("2026-10-20T10:00:00Z"), JsonMapper.builder().build().readTree("{}"));

            handler.handle(envelope);

            verify(cancelPendingExecutionsUseCase).execute(commandCaptor.capture());
            CancelPendingExecutionsCommand command = commandCaptor.getValue();
            assertThat(command.eventId()).isEqualTo(EVENT_ID);
            assertThat(command.eventType()).isEqualTo(ServiceOrderCancelledHandler.TYPE);
            assertThat(command.serviceOrderId()).isEqualTo(SERVICE_ORDER_ID);
        }
    }
}