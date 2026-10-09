package br.com.ofisy.ms_ofisy_execution.application.listbystatus;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListByStatusServiceOrderExecutionUseCase {
    Page<ServiceOrderExecution> execute(String status, Pageable pageable);
}
