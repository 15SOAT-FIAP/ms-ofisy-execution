package br.com.ofisy.ms_ofisy_execution.adapters.gateways;

import br.com.ofisy.ms_ofisy_execution.domain.OutboxEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboxEventMapperTest {
    @Test
    void shouldConvertDomainToEntity() {
        OutboxEvent outboxEvent = OutboxEvent.create("ExecutionStarted", UUID.randomUUID(), Map.of());
        String payloadJson = "{\"executionId\":\"%s\"}".formatted(UUID.randomUUID());

        OutboxEventEntity response = OutboxEventMapper.toEntity(outboxEvent, payloadJson);

        assertNotNull(response);
        assertEquals(outboxEvent.getEventId(), response.getId());
        assertEquals(outboxEvent.getEventType(), response.getEventType());
        assertEquals(outboxEvent.getServiceOrderId(), response.getServiceOrderId());
        assertEquals(payloadJson, response.getPayload());
        assertEquals(outboxEvent.getOccurredAt(), response.getOccurredAt());
        assertNull(response.getPublishedAt());
        assertTrue(response.isNew());
    }
}