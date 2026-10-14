package br.com.ofisy.ms_ofisy_execution.adapters.listeners.handlers;

import br.com.ofisy.ms_ofisy_execution.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_execution.application.create.CreateServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.create.CreateServiceOrderExecutionUseCase.CreateServiceOrderExecutionCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteApprovedHandlerTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID SERVICE_ORDER_ID = UUID.randomUUID();
    private static final UUID FIRST_CATALOG_ID = UUID.randomUUID();
    private static final UUID SECOND_CATALOG_ID = UUID.randomUUID();

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Mock
    private CreateServiceOrderExecutionUseCase createServiceOrderExecutionUseCase;

    @Captor
    private ArgumentCaptor<CreateServiceOrderExecutionCommand> commandCaptor;

    private QuoteApprovedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new QuoteApprovedHandler(createServiceOrderExecutionUseCase, jsonMapper);
    }

    private EventEnvelope envelope(String payload) {
        JsonNode node = jsonMapper.readTree(payload);
        return new EventEnvelope(EVENT_ID, QuoteApprovedHandler.TYPE, SERVICE_ORDER_ID, Instant.parse("2026-10-13T10:00:00Z"), node);
    }

    @Test
    void shouldHandleQuoteApprovedType() {
        assertThat(handler.type()).isEqualTo("QuoteApproved");
    }

    @Nested
    class Handle {

        @Test
        void shouldMapPayloadToCreateCommand() {
            when(createServiceOrderExecutionUseCase.execute(any())).thenReturn(List.of());

            handler.handle(envelope("""
                    {
                      "quoteId": "%s",
                      "serviceItems": [
                        { "serviceCatalogId": "%s" },
                        { "serviceCatalogId": "%s" }
                      ]
                    }
                    """.formatted(UUID.randomUUID(), FIRST_CATALOG_ID, SECOND_CATALOG_ID)));

            verify(createServiceOrderExecutionUseCase).execute(commandCaptor.capture());
            CreateServiceOrderExecutionCommand command = commandCaptor.getValue();
            assertThat(command.eventId()).isEqualTo(EVENT_ID);
            assertThat(command.eventType()).isEqualTo(QuoteApprovedHandler.TYPE);
            assertThat(command.serviceOrderId()).isEqualTo(SERVICE_ORDER_ID);
            assertThat(command.serviceCatalogIds()).containsExactly(FIRST_CATALOG_ID, SECOND_CATALOG_ID);
        }

        @Test
        void shouldFailWhenPayloadIsInvalid() {
            EventEnvelope envelope = envelope("""
                    { "quoteId": "not-a-uuid", "serviceItems": [] }
                    """);

            assertThatThrownBy(() -> handler.handle(envelope))
                    .isInstanceOf(JacksonException.class);
            verifyNoInteractions(createServiceOrderExecutionUseCase);
        }
    }
}