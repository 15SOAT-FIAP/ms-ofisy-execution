package br.com.ofisy.ms_ofisy_execution.application.listbystatus;

import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import br.com.ofisy.ms_ofisy_execution.domain.exception.InvalidServiceOrderExecutionStatusException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListByStatusServiceOrderExecutionServiceTest {

    @Mock
    private ServiceOrderExecutionRepository repository;

    @InjectMocks
    private ListByStatusServiceOrderExecutionService listByStatusService;

    @Nested
    class Execute {

        @Test
        void shouldConvertStatusAndQueryRepository() {
            Pageable pageable = PageRequest.of(0, 10);
            var execution = createServiceOrderExecution(ServiceOrderExecutionStatus.IN_PROGRESS);
            Page<ServiceOrderExecution> page = new PageImpl<>(List.of(execution), pageable, 1);

            when(repository.findByStatus(ServiceOrderExecutionStatus.IN_PROGRESS, pageable)).thenReturn(page);

            Page<ServiceOrderExecution> result = listByStatusService.execute("IN_PROGRESS", pageable);

            assertThat(result.getContent()).containsExactly(execution);
            verify(repository).findByStatus(ServiceOrderExecutionStatus.IN_PROGRESS, pageable);
        }

        @ParameterizedTest
        @ValueSource(strings = {"pending", "Pending", "PENDING"})
        void shouldAcceptStatusCaseInsensitive(String status) {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ServiceOrderExecution> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(repository.findByStatus(ServiceOrderExecutionStatus.PENDING, pageable)).thenReturn(emptyPage);

            listByStatusService.execute(status, pageable);

            verify(repository).findByStatus(ServiceOrderExecutionStatus.PENDING, pageable);
        }

        @Test
        void shouldThrowInvalidStatusExceptionAndNotQueryRepositoryWhenStatusIsUnknown() {
            Pageable pageable = PageRequest.of(0, 10);

            assertThatThrownBy(() -> listByStatusService.execute("FINISHED", pageable))
                    .isInstanceOf(InvalidServiceOrderExecutionStatusException.class)
                    .hasMessage("Status inválido: FINISHED");

            verifyNoInteractions(repository);
        }

        @Test
        void shouldForwardPageableAndKeepPaginationInfo() {
            Pageable pageable = PageRequest.of(2, 5, Sort.by("finishedAt").descending());
            var execution = createServiceOrderExecution(ServiceOrderExecutionStatus.COMPLETED);
            Page<ServiceOrderExecution> page = new PageImpl<>(List.of(execution), pageable, 11);

            when(repository.findByStatus(ServiceOrderExecutionStatus.COMPLETED, pageable)).thenReturn(page);

            Page<ServiceOrderExecution> result = listByStatusService.execute("completed", pageable);

            assertThat(result.getNumber()).isEqualTo(2);
            assertThat(result.getSize()).isEqualTo(5);
            assertThat(result.getSort()).isEqualTo(Sort.by("finishedAt").descending());
            assertThat(result.getTotalElements()).isEqualTo(11);
            assertThat(result.getTotalPages()).isEqualTo(3);
        }
    }

    private ServiceOrderExecution createServiceOrderExecution(ServiceOrderExecutionStatus status) {
        return ServiceOrderExecution.reconstruct(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                status,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null
        );
    }
}