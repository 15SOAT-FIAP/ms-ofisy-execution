package br.com.ofisy.ms_ofisy_execution.application.cancelpending;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CancelPendingExecutionsService implements CancelPendingExecutionsUseCase {

    private final ServiceOrderExecutionRepository repository;

    @Override
    public void execute(UUID serviceOrderId) {
        List<ServiceOrderExecutionStatus> activeStatuses = List.of(
                ServiceOrderExecutionStatus.PENDING,
                ServiceOrderExecutionStatus.IN_PROGRESS
        );
        repository.findByServiceOrderIdAndStatusIn(serviceOrderId, activeStatuses)
                .forEach(execution -> {
                    execution.cancel();
                    repository.save(execution);
                });
    }
}
