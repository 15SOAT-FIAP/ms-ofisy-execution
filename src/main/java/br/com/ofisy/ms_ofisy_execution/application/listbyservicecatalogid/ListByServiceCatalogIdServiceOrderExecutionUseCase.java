package br.com.ofisy.ms_ofisy_execution.application.listbyservicecatalogid;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ListByServiceCatalogIdServiceOrderExecutionUseCase {
    Page<ServiceOrderExecution> execute(UUID serviceCatalogId, Pageable pageable);
}
