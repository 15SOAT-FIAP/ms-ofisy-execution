package br.com.ofisy.ms_ofisy_execution.application.cancelpending;

import br.com.ofisy.ms_ofisy_execution.domain.ProcessedEvent;
import br.com.ofisy.ms_ofisy_execution.domain.ProcessedEventRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CancelPendingExecutionsService implements CancelPendingExecutionsUseCase {

    private final ServiceOrderExecutionRepository repository;
    private final ProcessedEventRepository processedEventRepository;

    @Override
    public void execute(CancelPendingExecutionsCommand cmd) {
        if (processedEventRepository.existsById(cmd.eventId())) {
            return;
        }

        List<ServiceOrderExecutionStatus> activeStatuses = List.of(
                ServiceOrderExecutionStatus.PENDING,
                ServiceOrderExecutionStatus.IN_PROGRESS
        );
        repository.findByServiceOrderIdAndStatusIn(cmd.serviceOrderId(), activeStatuses)
                .forEach(execution -> {
                    execution.cancel();
                    repository.save(execution);
                });

        processedEventRepository.save(ProcessedEvent.create(cmd.eventId(), cmd.eventType()));
    }
}