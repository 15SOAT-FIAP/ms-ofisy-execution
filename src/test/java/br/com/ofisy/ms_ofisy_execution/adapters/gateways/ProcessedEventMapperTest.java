package br.com.ofisy.ms_ofisy_execution.adapters.gateways;

import br.com.ofisy.ms_ofisy_execution.domain.ProcessedEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessedEventMapperTest {
    @Test
    void shouldConvertEntityToDomain() {
        UUID eventId = UUID.randomUUID();
        LocalDateTime processedAt = LocalDateTime.now();

        ProcessedEventEntity entity = ProcessedEventEntity.builder()
                .eventId(eventId)
                .eventType("QuoteApproved")
                .processedAt(processedAt)
                .build();

        ProcessedEvent processedEvent = ProcessedEventMapper.toDomain(entity);

        assertNotNull(processedEvent);
        assertEquals(eventId, processedEvent.getEventId());
        assertEquals("QuoteApproved", processedEvent.getEventType());
        assertEquals(processedAt, processedEvent.getProcessedAt());
    }

    @Test
    void shouldConvertDomainToEntity() {
        ProcessedEvent processedEvent = ProcessedEvent.create(UUID.randomUUID(), "QuoteApproved");

        ProcessedEventEntity response = ProcessedEventMapper.toEntity(processedEvent);

        assertNotNull(response);
        assertEquals(processedEvent.getEventId(), response.getId());
        assertEquals(processedEvent.getEventType(), response.getEventType());
        assertEquals(processedEvent.getProcessedAt(), response.getProcessedAt());
        assertTrue(response.isNew());
    }
}