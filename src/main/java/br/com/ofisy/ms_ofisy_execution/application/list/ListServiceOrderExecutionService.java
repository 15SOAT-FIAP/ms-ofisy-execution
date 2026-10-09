package br.com.ofisy.ms_ofisy_execution.application.list;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListServiceOrderExecutionService implements ListServiceOrderExecutionUseCase {

    private final ServiceOrderExecutionRepository repository;

    @Override
    public Page<ServiceOrderExecution> execute(Pageable pageable) {
        return repository.findAll(pageable);
    }
}
