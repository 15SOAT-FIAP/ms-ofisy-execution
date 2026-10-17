package br.com.ofisy.ms_ofisy_execution.adapters.presenters;

import br.com.ofisy.ms_ofisy_execution.adapters.controllers.dto.ServiceOrderExecutionResponseDTO;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceOrderExecutionPresenter {

    public static ServiceOrderExecutionResponseDTO present(ServiceOrderExecution serviceOrderExecution) {
        return new ServiceOrderExecutionResponseDTO(
                serviceOrderExecution.getId(),
                serviceOrderExecution.getServiceCatalogId(),
                serviceOrderExecution.getServiceOrderId(),
                serviceOrderExecution.getStatus(),
                serviceOrderExecution.getCreatedAt(),
                serviceOrderExecution.getUpdatedAt(),
                serviceOrderExecution.getStartedAt(),
                serviceOrderExecution.getFinishedAt()
        );
    }
}