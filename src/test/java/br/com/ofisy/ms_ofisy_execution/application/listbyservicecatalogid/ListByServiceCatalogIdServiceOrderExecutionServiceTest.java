package br.com.ofisy.ms_ofisy_execution.application.listbyservicecatalogid;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListByServiceCatalogIdServiceOrderExecutionServiceTest {

    private static final UUID SERVICE_CATALOG_ID = UUID.randomUUID();

    @Mock
    private ServiceOrderExecutionRepository repository;

    @InjectMocks
    private ListByServiceCatalogIdServiceOrderExecutionService listByServiceCatalogIdService;

    @Nested
    class Execute {

        @Test
        void shouldReturnExecutionsOfGivenServiceCatalog() {
            Pageable pageable = PageRequest.of(0, 10);
            var execution1 = createServiceOrderExecution();
            var execution2 = createServiceOrderExecution();
            Page<ServiceOrderExecution> page = new PageImpl<>(List.of(execution1, execution2), pageable, 2);

            when(repository.findByServiceCatalogId(SERVICE_CATALOG_ID, pageable)).thenReturn(page);

            Page<ServiceOrderExecution> result = listByServiceCatalogIdService.execute(SERVICE_CATALOG_ID, pageable);

            assertThat(result.getContent()).containsExactly(execution1, execution2);
            assertThat(result.getContent())
                    .extracting(ServiceOrderExecution::getServiceCatalogId)
                    .containsOnly(SERVICE_CATALOG_ID);
            verify(repository).findByServiceCatalogId(SERVICE_CATALOG_ID, pageable);
            verifyNoMoreInteractions(repository);
        }

        @Test
        void shouldReturnEmptyPageWhenServiceCatalogHasNoExecutions() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ServiceOrderExecution> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(repository.findByServiceCatalogId(SERVICE_CATALOG_ID, pageable)).thenReturn(emptyPage);

            Page<ServiceOrderExecution> result = listByServiceCatalogIdService.execute(SERVICE_CATALOG_ID, pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        void shouldForwardPageableAndKeepPaginationInfo() {
            Pageable pageable = PageRequest.of(2, 5, Sort.by("createdAt").descending());
            var execution = createServiceOrderExecution();
            Page<ServiceOrderExecution> page = new PageImpl<>(List.of(execution), pageable, 11);

            when(repository.findByServiceCatalogId(SERVICE_CATALOG_ID, pageable)).thenReturn(page);

            Page<ServiceOrderExecution> result = listByServiceCatalogIdService.execute(SERVICE_CATALOG_ID, pageable);

            assertThat(result.getNumber()).isEqualTo(2);
            assertThat(result.getSize()).isEqualTo(5);
            assertThat(result.getSort()).isEqualTo(Sort.by("createdAt").descending());
            assertThat(result.getTotalElements()).isEqualTo(11);
            assertThat(result.getTotalPages()).isEqualTo(3);
        }
    }

    private ServiceOrderExecution createServiceOrderExecution() {
        return ServiceOrderExecution.reconstruct(
                UUID.randomUUID(),
                SERVICE_CATALOG_ID,
                UUID.randomUUID(),
                ServiceOrderExecutionStatus.PENDING,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null
        );
    }
}