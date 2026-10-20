package br.com.ofisy.ms_ofisy_execution.adapters.gateways;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JpaServiceOrderExecutionRepository extends JpaRepository<ServiceOrderExecutionEntity, UUID> {

    Page<ServiceOrderExecutionEntity> findByServiceCatalogId(UUID serviceCatalogId, Pageable pageable);

    Page<ServiceOrderExecutionEntity> findByStatus(ServiceOrderExecutionStatus status, Pageable pageable);

    Page<ServiceOrderExecutionEntity> findByServiceOrderId(UUID serviceOrderId, Pageable pageable);

    long countByServiceOrderId(UUID serviceOrderId);

    long countByServiceOrderIdAndStatus(UUID serviceOrderId, ServiceOrderExecutionStatus serviceOrderExecutionStatus);

    List<ServiceOrderExecutionEntity> findByServiceOrderIdAndStatusIn(UUID serviceOrderId, Collection<ServiceOrderExecutionStatus> statuses);

    boolean existsByServiceOrderIdAndServiceCatalogId(UUID serviceOrderId, UUID serviceCatalogId);

    boolean existsByServiceOrderIdAndStartedAtIsNotNull(UUID serviceOrderId);

    // Ordenado por id para que conclusoes concorrentes travem as linhas na mesma ordem e nao entrem em deadlock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e from ServiceOrderExecutionEntity e
            where e.serviceOrderId = (select x.serviceOrderId from ServiceOrderExecutionEntity x where x.id = :executionId)
            order by e.id""")
    List<ServiceOrderExecutionEntity> lockAllFromSameServiceOrder(UUID executionId);
}
