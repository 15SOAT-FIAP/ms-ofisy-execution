package br.com.ofisy.ms_ofisy_execution.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class OutboxEventTest {

    @Nested
    class CreateOutboxEvent {
        @Test
        void shouldCreateOutboxEventWithGeneratedIdAndOccurredAt() {
            var serviceOrderId = UUID.randomUUID();
            var payload = Map.of("executionId", UUID.randomUUID());

            var result = OutboxEvent.create("ExecutionStarted", serviceOrderId, payload);

            assertThat(result.getEventId()).isNotNull();
            assertThat(result.getEventType()).isEqualTo("ExecutionStarted");
            assertThat(result.getServiceOrderId()).isEqualTo(serviceOrderId);
            assertThat(result.getPayload()).isEqualTo(payload);
            assertThat(result.getOccurredAt()).isNotNull();
        }

        @Test
        void shouldGenerateDifferentIdsForEachEvent() {
            var serviceOrderId = UUID.randomUUID();

            var first = OutboxEvent.create("ExecutionStarted", serviceOrderId, Map.of());
            var second = OutboxEvent.create("ExecutionStarted", serviceOrderId, Map.of());

            assertThat(first.getEventId()).isNotEqualTo(second.getEventId());
        }
    }
}