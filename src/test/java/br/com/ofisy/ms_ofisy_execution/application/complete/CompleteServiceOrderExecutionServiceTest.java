package br.com.ofisy.ms_ofisy_execution.application.complete;

import br.com.ofisy.ms_ofisy_execution.application.exception.ServiceOrderExecutionNotFoundException;
import br.com.ofisy.ms_ofisy_execution.domain.OutboxEvent;
import br.com.ofisy.ms_ofisy_execution.domain.OutboxEventRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompleteServiceOrderExecutionServiceTest {

    private static final UUID VALID_ID = UUID.randomUUID();
    private static final UUID SERVICE_ORDER_ID = UUID.randomUUID();

    @Mock
    private ServiceOrderExecutionRepository repository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    @InjectMocks
    private CompleteServiceOrderExecutionService completeService;

    @Nested
    class Execute {

        @Test
        void shouldCompleteExecutionAndRecordAllExecutionsCompletedWhenItIsTheLastOfTheServiceOrder() {
            var execution = createServiceOrderExecution(VALID_ID, ServiceOrderExecutionStatus.IN_PROGRESS);
            var sibling = createServiceOrderExecution(UUID.randomUUID(), ServiceOrderExecutionStatus.COMPLETED);
            when(repository.lockAllFromSameServiceOrder(VALID_ID)).thenReturn(List.of(sibling, execution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServiceOrderExecution result = completeService.execute(VALID_ID);

            assertThat(result.getStatus()).isEqualTo(ServiceOrderExecutionStatus.COMPLETED);
            assertThat(result.getFinishedAt()).isNotNull();
            verify(outboxEventRepository).save(outboxEventCaptor.capture());
            OutboxEvent event = outboxEventCaptor.getValue();
            assertThat(event.getEventType()).isEqualTo(CompleteServiceOrderExecutionService.ALL_EXECUTIONS_COMPLETED);
            assertThat(event.getServiceOrderId()).isEqualTo(SERVICE_ORDER_ID);
            assertThat(event.getPayload()).isEqualTo(Map.of());
        }

        @Test
        void shouldRecordAllExecutionsCompletedWhenServiceOrderHasASingleExecution() {
            var execution = createServiceOrderExecution(VALID_ID, ServiceOrderExecutionStatus.IN_PROGRESS);
            when(repository.lockAllFromSameServiceOrder(VALID_ID)).thenReturn(List.of(execution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            completeService.execute(VALID_ID);

            verify(outboxEventRepository).save(any());
        }

        @Test
        void shouldCompleteExecutionWithoutRecordingEventWhenServiceOrderStillHasPendingExecutions() {
            var execution = createServiceOrderExecution(VALID_ID, ServiceOrderExecutionStatus.IN_PROGRESS);
            var inProgress = createServiceOrderExecution(UUID.randomUUID(), ServiceOrderExecutionStatus.IN_PROGRESS);
            var pending = createServiceOrderExecution(UUID.randomUUID(), ServiceOrderExecutionStatus.PENDING);
            when(repository.lockAllFromSameServiceOrder(VALID_ID)).thenReturn(List.of(execution, inProgress, pending));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServiceOrderExecution result = completeService.execute(VALID_ID);

            assertThat(result.getStatus()).isEqualTo(ServiceOrderExecutionStatus.COMPLETED);
            verifyNoInteractions(outboxEventRepository);
        }

        @Test
        void shouldNotRecordEventWhenAnotherExecutionOfTheServiceOrderIsCancelled() {
            var execution = createServiceOrderExecution(VALID_ID, ServiceOrderExecutionStatus.IN_PROGRESS);
            var cancelled = createServiceOrderExecution(UUID.randomUUID(), ServiceOrderExecutionStatus.CANCELLED);
            when(repository.lockAllFromSameServiceOrder(VALID_ID)).thenReturn(List.of(execution, cancelled));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            completeService.execute(VALID_ID);

            verifyNoInteractions(outboxEventRepository);
        }

        @Test
        void shouldThrowServiceOrderExecutionNotFoundExceptionWhenNotFound() {
            when(repository.lockAllFromSameServiceOrder(VALID_ID)).thenReturn(List.of());

            assertThatThrownBy(() -> completeService.execute(VALID_ID))
                    .isInstanceOf(ServiceOrderExecutionNotFoundException.class);

            verify(repository, never()).save(any());
            verifyNoInteractions(outboxEventRepository);
        }

        @Test
        void shouldThrowInvalidTransitionWhenExecutionIsNotInProgress() {
            var execution = createServiceOrderExecution(VALID_ID, ServiceOrderExecutionStatus.PENDING);
            when(repository.lockAllFromSameServiceOrder(VALID_ID)).thenReturn(List.of(execution));

            assertThatThrownBy(() -> completeService.execute(VALID_ID))
                    .isInstanceOf(InvalidServiceOrderExecutionTransitionException.class);

            verify(repository, never()).save(any());
            verifyNoInteractions(outboxEventRepository);
        }
    }

    private ServiceOrderExecution createServiceOrderExecution(UUID id, ServiceOrderExecutionStatus status) {
        return ServiceOrderExecution.reconstruct(
                id,
                UUID.randomUUID(),
                SERVICE_ORDER_ID,
                status,
                LocalDateTime.now(),
                LocalDateTime.now(),
                status == ServiceOrderExecutionStatus.PENDING ? null : LocalDateTime.now(),
                null
        );
    }
}