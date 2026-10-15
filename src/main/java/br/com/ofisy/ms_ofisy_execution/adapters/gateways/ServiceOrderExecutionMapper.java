package br.com.ofisy.ms_ofisy_execution.adapters.gateways;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceOrderExecutionMapper {

    public static ServiceOrderExecution toDomain(ServiceOrderExecutionEntity entity) {
        return ServiceOrderExecution.reconstruct(
                entity.getId(),
                entity.getServiceCatalogId(),
                entity.getServiceOrderId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getStartedAt(),
                entity.getFinishedAt()
        );
    }

    public static ServiceOrderExecutionEntity toEntity(ServiceOrderExecution domain) {
        return ServiceOrderExecutionEntity.builder()
                .id(domain.getId())
                .serviceCatalogId(domain.getServiceCatalogId())
                .serviceOrderId(domain.getServiceOrderId())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .startedAt(domain.getStartedAt())
                .finishedAt(domain.getFinishedAt())
                .build();
    }
}
