package br.com.ofisy.ms_ofisy_execution.application.complete;

import br.com.ofisy.ms_ofisy_execution.application.exception.ServiceOrderExecutionNotFoundException;
import br.com.ofisy.ms_ofisy_execution.domain.OutboxEvent;
import br.com.ofisy.ms_ofisy_execution.domain.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CompleteServiceOrderExecutionService implements CompleteServiceOrderExecutionUseCase {

    public static final String ALL_EXECUTIONS_COMPLETED = "AllExecutionsCompleted";

    private final ServiceOrderExecutionRepository repository;
    private final OutboxEventRepository outboxEventRepository;

    @Override
    public ServiceOrderExecution execute(UUID id) {
        // Trava todas as execucoes da OS para que a ultima conclusao sempre enxergue as anteriores
        List<ServiceOrderExecution> executions = repository.lockAllFromSameServiceOrder(id);
        ServiceOrderExecution execution = executions.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ServiceOrderExecutionNotFoundException(id));

        execution.complete();
        ServiceOrderExecution savedExecution = repository.save(execution);

        if (allCompleted(executions)) {
            outboxEventRepository.save(OutboxEvent.create(ALL_EXECUTIONS_COMPLETED, savedExecution.getServiceOrderId(), Map.of()));
        }

        return savedExecution;
    }

    private boolean allCompleted(List<ServiceOrderExecution> executions) {
        return executions.stream().allMatch(e -> e.getStatus() == ServiceOrderExecutionStatus.COMPLETED);
    }
}