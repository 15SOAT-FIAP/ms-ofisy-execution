package br.com.ofisy.ms_ofisy_execution.application.start;

import br.com.ofisy.ms_ofisy_execution.application.exception.ServiceOrderExecutionNotFoundException;
import br.com.ofisy.ms_ofisy_execution.domain.OutboxEvent;
import br.com.ofisy.ms_ofisy_execution.domain.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class StartServiceOrderExecutionService implements StartServiceOrderExecutionUseCase {

    public static final String EXECUTION_STARTED = "ExecutionStarted";

    private final ServiceOrderExecutionRepository repository;
    private final OutboxEventRepository outboxEventRepository;

    @Override
    public ServiceOrderExecution execute(UUID id) {
        ServiceOrderExecution execution = repository.findById(id)
                .orElseThrow(() -> new ServiceOrderExecutionNotFoundException(id));
        // O core so move a OS para IN_PROGRESS no inicio da primeira execucao
        boolean firstOfServiceOrder = !repository.existsByServiceOrderIdAndStartedAtIsNotNull(execution.getServiceOrderId());

        execution.start();
        ServiceOrderExecution savedExecution = repository.save(execution);

        if (firstOfServiceOrder) {
            outboxEventRepository.save(OutboxEvent.create(EXECUTION_STARTED, savedExecution.getServiceOrderId(), Map.of()));
        }

        return savedExecution;
    }
}