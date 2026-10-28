package br.com.ofisy.ms_ofisy_execution.application.create;

import br.com.ofisy.ms_ofisy_execution.domain.ProcessedEvent;
import br.com.ofisy.ms_ofisy_execution.domain.ProcessedEventRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateServiceOrderExecutionService implements CreateServiceOrderExecutionUseCase {

    private final ServiceOrderExecutionRepository repository;
    private final ProcessedEventRepository processedEventRepository;

    @Override
    public List<ServiceOrderExecution> execute(CreateServiceOrderExecutionCommand cmd) {
        if (processedEventRepository.existsById(cmd.eventId())) {
            return List.of();
        }

        List<ServiceOrderExecution> executions = cmd.serviceCatalogIds().stream()
                .distinct()
                .filter(catalogId -> !repository.existsByServiceOrderIdAndServiceCatalogId(cmd.serviceOrderId(), catalogId))
                .map(catalogId -> repository.save(ServiceOrderExecution.create(catalogId, cmd.serviceOrderId())))
                .toList();

        processedEventRepository.save(ProcessedEvent.create(cmd.eventId(), cmd.eventType()));
        return executions;
    }
}