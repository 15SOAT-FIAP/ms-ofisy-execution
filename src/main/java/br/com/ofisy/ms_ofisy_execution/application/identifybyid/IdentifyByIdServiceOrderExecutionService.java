package br.com.ofisy.ms_ofisy_execution.application.identifybyid;

import br.com.ofisy.ms_ofisy_execution.application.exception.ServiceOrderExecutionNotFoundException;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IdentifyByIdServiceOrderExecutionService implements IdentifyByIdServiceOrderExecutionUseCase {

    private final ServiceOrderExecutionRepository repository;

    @Override
    public ServiceOrderExecution execute(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ServiceOrderExecutionNotFoundException(id));
    }
}
