package br.com.ofisy.ms_ofisy_execution.application.listbystatus;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListByStatusServiceOrderExecutionService implements ListByStatusServiceOrderExecutionUseCase {

    private final ServiceOrderExecutionRepository repository;

    @Override
    public Page<ServiceOrderExecution> execute(String status, Pageable pageable) {
        ServiceOrderExecutionStatus executionStatus =
                ServiceOrderExecutionStatus.from(status);

        return repository.findByStatus(executionStatus, pageable);
    }
}
