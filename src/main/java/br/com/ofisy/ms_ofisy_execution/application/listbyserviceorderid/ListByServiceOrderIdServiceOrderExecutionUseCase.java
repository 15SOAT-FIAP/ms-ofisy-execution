package br.com.ofisy.ms_ofisy_execution.application.listbyserviceorderid;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ListByServiceOrderIdServiceOrderExecutionUseCase {
    Page<ServiceOrderExecution> execute(UUID serviceOrderId, Pageable pageable);
}
