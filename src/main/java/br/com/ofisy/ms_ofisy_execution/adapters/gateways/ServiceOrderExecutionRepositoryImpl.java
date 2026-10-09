package br.com.ofisy.ms_ofisy_execution.adapters.gateways;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ServiceOrderExecutionRepositoryImpl implements ServiceOrderExecutionRepository {

    private final JpaServiceOrderExecutionRepository jpa;

    @Override
    public ServiceOrderExecution save(ServiceOrderExecution serviceOrderExecution) {
        ServiceOrderExecutionEntity entity = ServiceOrderExecutionMapper.toEntity(serviceOrderExecution);
        ServiceOrderExecutionEntity savedEntity = jpa.save(entity);
        return ServiceOrderExecutionMapper.toDomain(savedEntity);
    }

    @Override
    public Page<ServiceOrderExecution> findAll(Pageable pageable) {
        return jpa.findAll(pageable)
                .map(ServiceOrderExecutionMapper::toDomain);
    }

    @Override
    public Optional<ServiceOrderExecution> findById(UUID id) {
        return jpa.findById(id)
                .map(ServiceOrderExecutionMapper::toDomain);
    }

    @Override
    public Page<ServiceOrderExecution> findByServiceCatalogId(UUID serviceCatalogId, Pageable pageable) {
        return jpa.findByServiceCatalogId(serviceCatalogId, pageable)
                .map(ServiceOrderExecutionMapper::toDomain);
    }

    @Override
    public Page<ServiceOrderExecution> findByServiceOrderId(UUID serviceOrderId, Pageable pageable) {
        return jpa.findByServiceOrderId(serviceOrderId, pageable)
                .map(ServiceOrderExecutionMapper::toDomain);
    }

    @Override
    public Page<ServiceOrderExecution> findByStatus(ServiceOrderExecutionStatus status, Pageable pageable) {
        return jpa.findByStatus(status, pageable)
                .map(ServiceOrderExecutionMapper::toDomain);
    }

    @Override
    public long countByServiceOrderId(UUID serviceOrderId) {
        return jpa.countByServiceOrderId(serviceOrderId);
    }

    @Override
    public long countByServiceOrderIdAndStatus(UUID serviceOrderId, ServiceOrderExecutionStatus serviceOrderExecutionStatus) {
        return jpa.countByServiceOrderIdAndStatus(serviceOrderId, serviceOrderExecutionStatus);
    }

    @Override
    public List<ServiceOrderExecution> findByServiceOrderIdAndStatusIn(UUID serviceOrderId, Collection<ServiceOrderExecutionStatus> statuses) {
        return jpa.findByServiceOrderIdAndStatusIn(serviceOrderId, statuses)
                .stream()
                .map(ServiceOrderExecutionMapper::toDomain)
                .toList();
    }
}
