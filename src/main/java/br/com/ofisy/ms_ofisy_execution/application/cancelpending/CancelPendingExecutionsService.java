package br.com.ofisy.ms_ofisy_execution.application.cancelpending;

import br.com.ofisy.ms_ofisy_execution.domain.OutboxEvent;
import br.com.ofisy.ms_ofisy_execution.domain.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ProcessedEvent;
import br.com.ofisy.ms_ofisy_execution.domain.ProcessedEventRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class CancelPendingExecutionsService implements CancelPendingExecutionsUseCase {

    public static final String ALL_EXECUTIONS_CANCELLED = "AllExecutionsCancelled";

    private final ServiceOrderExecutionRepository repository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Override
    public void execute(CancelPendingExecutionsCommand cmd) {
        if (processedEventRepository.existsById(cmd.eventId())) {
            return;
        }

        List<ServiceOrderExecutionStatus> activeStatuses = List.of(
                ServiceOrderExecutionStatus.PENDING,
                ServiceOrderExecutionStatus.IN_PROGRESS
        );
        repository.lockByServiceOrderIdAndStatusIn(cmd.serviceOrderId(), activeStatuses)
                .forEach(execution -> {
                    execution.cancel();
                    repository.save(execution);
                });

        outboxEventRepository.save(OutboxEvent.create(ALL_EXECUTIONS_CANCELLED, cmd.serviceOrderId(), Map.of()));
        processedEventRepository.save(ProcessedEvent.create(cmd.eventId(), cmd.eventType()));
    }
}