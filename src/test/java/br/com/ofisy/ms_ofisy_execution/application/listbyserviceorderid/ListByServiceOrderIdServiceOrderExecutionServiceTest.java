package br.com.ofisy.ms_ofisy_execution.application.listbyserviceorderid;

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
class ListByServiceOrderIdServiceOrderExecutionServiceTest {

    private static final UUID SERVICE_ORDER_ID = UUID.randomUUID();

    @Mock
    private ServiceOrderExecutionRepository repository;

    @InjectMocks
    private ListByServiceOrderIdServiceOrderExecutionService listByServiceOrderIdService;

    @Nested
    class Execute {

        @Test
        void shouldReturnExecutionsOfGivenServiceOrder() {
            Pageable pageable = PageRequest.of(0, 10);
            var pending = createServiceOrderExecution(ServiceOrderExecutionStatus.PENDING);
            var completed = createServiceOrderExecution(ServiceOrderExecutionStatus.COMPLETED);
            Page<ServiceOrderExecution> page = new PageImpl<>(List.of(pending, completed), pageable, 2);

            when(repository.findByServiceOrderId(SERVICE_ORDER_ID, pageable)).thenReturn(page);

            Page<ServiceOrderExecution> result = listByServiceOrderIdService.execute(SERVICE_ORDER_ID, pageable);

            assertThat(result.getContent()).containsExactly(pending, completed);
            assertThat(result.getContent())
                    .extracting(ServiceOrderExecution::getServiceOrderId)
                    .containsOnly(SERVICE_ORDER_ID);
            verify(repository).findByServiceOrderId(SERVICE_ORDER_ID, pageable);
            verifyNoMoreInteractions(repository);
        }

        @Test
        void shouldReturnEmptyPageWhenServiceOrderHasNoExecutions() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ServiceOrderExecution> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(repository.findByServiceOrderId(SERVICE_ORDER_ID, pageable)).thenReturn(emptyPage);

            Page<ServiceOrderExecution> result = listByServiceOrderIdService.execute(SERVICE_ORDER_ID, pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        void shouldForwardPageableAndKeepPaginationInfo() {
            Pageable pageable = PageRequest.of(1, 3, Sort.by("createdAt").ascending());
            var execution = createServiceOrderExecution(ServiceOrderExecutionStatus.IN_PROGRESS);
            Page<ServiceOrderExecution> page = new PageImpl<>(List.of(execution), pageable, 7);

            when(repository.findByServiceOrderId(SERVICE_ORDER_ID, pageable)).thenReturn(page);

            Page<ServiceOrderExecution> result = listByServiceOrderIdService.execute(SERVICE_ORDER_ID, pageable);

            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(3);
            assertThat(result.getSort()).isEqualTo(Sort.by("createdAt").ascending());
            assertThat(result.getTotalElements()).isEqualTo(7);
            assertThat(result.getTotalPages()).isEqualTo(3);
        }
    }

    private ServiceOrderExecution createServiceOrderExecution(ServiceOrderExecutionStatus status) {
        return ServiceOrderExecution.reconstruct(
                UUID.randomUUID(),
                UUID.randomUUID(),
                SERVICE_ORDER_ID,
                status,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null
        );
    }
}