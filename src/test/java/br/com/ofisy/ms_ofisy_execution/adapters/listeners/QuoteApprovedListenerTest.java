package br.com.ofisy.ms_ofisy_execution.adapters.listeners;

import br.com.ofisy.ms_ofisy_execution.adapters.listeners.dto.QuoteApprovedEvent;
import br.com.ofisy.ms_ofisy_execution.application.create.CreateServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.create.CreateServiceOrderExecutionUseCase.CreateServiceOrderExecutionCommand;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class QuoteApprovedListenerTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID SERVICE_ORDER_ID = UUID.randomUUID();
    private static final UUID FIRST_CATALOG_ID = UUID.randomUUID();
    private static final UUID SECOND_CATALOG_ID = UUID.randomUUID();

    @Mock
    private CreateServiceOrderExecutionUseCase createServiceOrderExecutionUseCase;

    @InjectMocks
    private QuoteApprovedListener listener;

    @Captor
    private ArgumentCaptor<CreateServiceOrderExecutionCommand> commandCaptor;

    private QuoteApprovedEvent event(String type) {
        return new QuoteApprovedEvent(
                EVENT_ID,
                type,
                SERVICE_ORDER_ID,
                Instant.parse("2026-10-13T10:00:00Z"),
                new QuoteApprovedEvent.Payload(
                        UUID.randomUUID(),
                        List.of(
                                new QuoteApprovedEvent.ServiceItem(FIRST_CATALOG_ID),
                                new QuoteApprovedEvent.ServiceItem(SECOND_CATALOG_ID)
                        )
                )
        );
    }

    @Nested
    class OnMessage {

        @Test
        void shouldMapEventToCreateCommand() {
            when(createServiceOrderExecutionUseCase.execute(any())).thenReturn(List.of());

            listener.onMessage(event(QuoteApprovedEvent.TYPE));

            verify(createServiceOrderExecutionUseCase).execute(commandCaptor.capture());
            CreateServiceOrderExecutionCommand command = commandCaptor.getValue();
            assertThat(command.eventId()).isEqualTo(EVENT_ID);
            assertThat(command.eventType()).isEqualTo(QuoteApprovedEvent.TYPE);
            assertThat(command.serviceOrderId()).isEqualTo(SERVICE_ORDER_ID);
            assertThat(command.serviceCatalogIds()).containsExactly(FIRST_CATALOG_ID, SECOND_CATALOG_ID);
        }

        @Test
        void shouldIgnoreEventOfAnotherType() {
            listener.onMessage(event("QuoteRejected"));

            verifyNoInteractions(createServiceOrderExecutionUseCase);
        }
    }
}