package br.com.ofisy.ms_ofisy_execution.application.list;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListServiceOrderExecutionUseCase {
    Page<ServiceOrderExecution> execute(Pageable pageable);
}
