package br.com.ofisy.ms_ofisy_execution.adapters.gateways;

import br.com.ofisy.ms_ofisy_execution.domain.ProcessedEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProcessedEventMapper {

    public static ProcessedEvent toDomain(ProcessedEventEntity entity) {
        return ProcessedEvent.reconstruct(
                entity.getEventId(),
                entity.getEventType(),
                entity.getProcessedAt()
        );
    }

    public static ProcessedEventEntity toEntity(ProcessedEvent domain) {
        return ProcessedEventEntity.builder()
                .eventId(domain.getEventId())
                .eventType(domain.getEventType())
                .processedAt(domain.getProcessedAt())
                .build();
    }
}