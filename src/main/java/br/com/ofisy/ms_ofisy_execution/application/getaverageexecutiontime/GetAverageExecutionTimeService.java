package br.com.ofisy.ms_ofisy_execution.application.getaverageexecutiontime;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetAverageExecutionTimeService implements GetAverageExecutionTimeServiceOrderExecutionUseCase {

    private final ServiceOrderExecutionRepository repository;

    @Override
    public double execute(UUID serviceCatalogId) {
        return repository.averageExecutionMinutesByServiceCatalogIdAndStatus(serviceCatalogId,
                ServiceOrderExecutionStatus.COMPLETED);
    }
}