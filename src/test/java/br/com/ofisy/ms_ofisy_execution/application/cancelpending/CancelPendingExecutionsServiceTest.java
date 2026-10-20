package br.com.ofisy.ms_ofisy_execution.application.cancelpending;

import br.com.ofisy.ms_ofisy_execution.application.cancelpending.CancelPendingExecutionsUseCase.CancelPendingExecutionsCommand;
import br.com.ofisy.ms_ofisy_execution.domain.ProcessedEvent;
import br.com.ofisy.ms_ofisy_execution.domain.ProcessedEventRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import br.com.ofisy.ms_ofisy_execution.domain.exception.InvalidServiceOrderExecutionTransitionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelPendingExecutionsServiceTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID SERVICE_ORDER_ID = UUID.randomUUID();
    private static final CancelPendingExecutionsCommand COMMAND =
            new CancelPendingExecutionsCommand(EVENT_ID, "ServiceOrderCancelled", SERVICE_ORDER_ID);

    @Mock
    private ServiceOrderExecutionRepository repository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private CancelPendingExecutionsService cancelPendingService;

    @Captor
    private ArgumentCaptor<Collection<ServiceOrderExecutionStatus>> statusesCaptor;

    @Captor
    private ArgumentCaptor<ProcessedEvent> processedEventCaptor;

    @Nested
    class Execute {

        @Test
        void shouldQueryOnlyPendingAndInProgressStatuses() {
            when(repository.findByServiceOrderIdAndStatusIn(eq(SERVICE_ORDER_ID), anyCollection()))
                    .thenReturn(List.of());

            cancelPendingService.execute(COMMAND);

            verify(repository).findByServiceOrderIdAndStatusIn(eq(SERVICE_ORDER_ID), statusesCaptor.capture());
            assertThat(statusesCaptor.getValue())
                    .containsExactlyInAnyOrder(ServiceOrderExecutionStatus.PENDING, ServiceOrderExecutionStatus.IN_PROGRESS);
        }

        @Test
        void shouldCancelAndSaveEveryActiveExecution() {
            var pending = createServiceOrderExecution(ServiceOrderExecutionStatus.PENDING, null);
            var inProgress = createServiceOrderExecution(ServiceOrderExecutionStatus.IN_PROGRESS, LocalDateTime.now());
            when(repository.findByServiceOrderIdAndStatusIn(eq(SERVICE_ORDER_ID), anyCollection()))
                    .thenReturn(List.of(pending, inProgress));

            cancelPendingService.execute(COMMAND);

            assertThat(pending.getStatus()).isEqualTo(ServiceOrderExecutionStatus.CANCELLED);
            assertThat(pending.getFinishedAt()).isNotNull();
            assertThat(inProgress.getStatus()).isEqualTo(ServiceOrderExecutionStatus.CANCELLED);
            assertThat(inProgress.getFinishedAt()).isNotNull();
            verify(repository).save(pending);
            verify(repository).save(inProgress);
        }

        @Test
        void shouldPreserveStartedAtWhenCancellingInProgressExecution() {
            var startedAt = LocalDateTime.of(2026, 1, 1, 9, 0);
            var inProgress = createServiceOrderExecution(ServiceOrderExecutionStatus.IN_PROGRESS, startedAt);
            when(repository.findByServiceOrderIdAndStatusIn(eq(SERVICE_ORDER_ID), anyCollection()))
                    .thenReturn(List.of(inProgress));

            cancelPendingService.execute(COMMAND);

            assertThat(inProgress.getStartedAt()).isEqualTo(startedAt);
            assertThat(inProgress.getFinishedAt()).isAfter(startedAt);
        }

        @Test
        void shouldNotSaveWhenNoActiveExecutionsFound() {
            when(repository.findByServiceOrderIdAndStatusIn(eq(SERVICE_ORDER_ID), anyCollection()))
                    .thenReturn(List.of());

            cancelPendingService.execute(COMMAND);

            verify(repository, never()).save(any());
        }

        @Test
        void shouldPropagateTransitionExceptionAndNotSaveWhenExecutionCannotBeCancelled() {
            var completed = createServiceOrderExecution(ServiceOrderExecutionStatus.COMPLETED, LocalDateTime.now());
            when(repository.findByServiceOrderIdAndStatusIn(eq(SERVICE_ORDER_ID), anyCollection()))
                    .thenReturn(List.of(completed));

            assertThatThrownBy(() -> cancelPendingService.execute(COMMAND))
                    .isInstanceOf(InvalidServiceOrderExecutionTransitionException.class);

            assertThat(completed.getStatus()).isEqualTo(ServiceOrderExecutionStatus.COMPLETED);
            verify(repository, never()).save(any());
            verify(processedEventRepository, never()).save(any());
        }

        @Test
        void shouldRecordProcessedEventAfterCancelling() {
            when(repository.findByServiceOrderIdAndStatusIn(eq(SERVICE_ORDER_ID), anyCollection()))
                    .thenReturn(List.of(createServiceOrderExecution(ServiceOrderExecutionStatus.PENDING, null)));

            cancelPendingService.execute(COMMAND);

            verify(processedEventRepository).save(processedEventCaptor.capture());
            assertThat(processedEventCaptor.getValue().getEventId()).isEqualTo(EVENT_ID);
            assertThat(processedEventCaptor.getValue().getEventType()).isEqualTo("ServiceOrderCancelled");
        }

        @Test
        void shouldRecordProcessedEventEvenWhenNoActiveExecutionsFound() {
            when(repository.findByServiceOrderIdAndStatusIn(eq(SERVICE_ORDER_ID), anyCollection()))
                    .thenReturn(List.of());

            cancelPendingService.execute(COMMAND);

            verify(processedEventRepository).save(any());
        }

        @Test
        void shouldSkipWhenEventWasAlreadyProcessed() {
            when(processedEventRepository.existsById(EVENT_ID)).thenReturn(true);

            cancelPendingService.execute(COMMAND);

            verifyNoInteractions(repository);
            verify(processedEventRepository, never()).save(any());
        }
    }

    private ServiceOrderExecution createServiceOrderExecution(ServiceOrderExecutionStatus status, LocalDateTime startedAt) {
        return ServiceOrderExecution.reconstruct(
                UUID.randomUUID(),
                UUID.randomUUID(),
                SERVICE_ORDER_ID,
                status,
                LocalDateTime.now(),
                LocalDateTime.now(),
                startedAt,
                null
        );
    }
}