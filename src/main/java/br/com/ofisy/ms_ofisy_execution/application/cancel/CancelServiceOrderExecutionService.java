package br.com.ofisy.ms_ofisy_execution.application.cancel;

import br.com.ofisy.ms_ofisy_execution.application.exception.ServiceOrderExecutionNotFoundException;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CancelServiceOrderExecutionService implements CancelServiceOrderExecutionUseCase {

    private final ServiceOrderExecutionRepository repository;

    @Override
    public ServiceOrderExecution execute(UUID id) {
        ServiceOrderExecution execution = repository.findById(id)
                .orElseThrow(() -> new ServiceOrderExecutionNotFoundException(id));

        execution.cancel();
        return repository.save(execution);
    }
}
