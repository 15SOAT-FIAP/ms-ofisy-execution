package br.com.ofisy.ms_ofisy_execution.adapters.gateways;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query(value = """
            SELECT COALESCE(AVG(GREATEST(FLOOR(EXTRACT(EPOCH FROM finished_at - started_at) / 60), 0)), 0)
            FROM service_order_executions
            WHERE service_catalog_id = :serviceCatalogId
              AND status = :status
              AND started_at IS NOT NULL
              AND finished_at IS NOT NULL
            """, nativeQuery = true)
    double averageExecutionMinutesByServiceCatalogIdAndStatus(UUID serviceCatalogId, String status);
}
