package br.com.ofisy.ms_ofisy_execution.application.listbyserviceorderid;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListByServiceOrderIdServiceOrderExecutionService implements ListByServiceOrderIdServiceOrderExecutionUseCase {

    private final ServiceOrderExecutionRepository repository;

    @Override
    public Page<ServiceOrderExecution> execute(UUID serviceOrderId, Pageable pageable) {
        return repository.findByServiceOrderId(serviceOrderId, pageable);
    }
}
