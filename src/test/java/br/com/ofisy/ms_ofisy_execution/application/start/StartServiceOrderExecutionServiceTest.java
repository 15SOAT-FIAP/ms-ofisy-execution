package br.com.ofisy.ms_ofisy_execution.application.start;

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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartServiceOrderExecutionServiceTest {

    private static final UUID VALID_ID = UUID.randomUUID();
    private static final UUID SERVICE_ORDER_ID = UUID.randomUUID();
    private static final UUID SERVICE_CATALOG_ID = UUID.randomUUID();

    @Mock
    private ServiceOrderExecutionRepository repository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    @InjectMocks
    private StartServiceOrderExecutionService startService;

    @Nested
    class Execute {

        @Test
        void shouldStartExecutionAndRecordExecutionStartedWhenItIsTheFirstOfTheServiceOrder() {
            when(repository.findById(VALID_ID)).thenReturn(Optional.of(createServiceOrderExecution(ServiceOrderExecutionStatus.PENDING)));
            when(repository.existsByServiceOrderIdAndStartedAtIsNotNull(SERVICE_ORDER_ID)).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServiceOrderExecution result = startService.execute(VALID_ID);

            assertThat(result.getStatus()).isEqualTo(ServiceOrderExecutionStatus.IN_PROGRESS);
            assertThat(result.getStartedAt()).isNotNull();
            verify(outboxEventRepository).save(outboxEventCaptor.capture());
            OutboxEvent event = outboxEventCaptor.getValue();
            assertThat(event.getEventType()).isEqualTo(StartServiceOrderExecutionService.EXECUTION_STARTED);
            assertThat(event.getServiceOrderId()).isEqualTo(SERVICE_ORDER_ID);
            assertThat(event.getPayload()).isEqualTo(Map.of());
        }

        @Test
        void shouldStartExecutionWithoutRecordingEventWhenServiceOrderAlreadyHasStartedExecution() {
            when(repository.findById(VALID_ID)).thenReturn(Optional.of(createServiceOrderExecution(ServiceOrderExecutionStatus.PENDING)));
            when(repository.existsByServiceOrderIdAndStartedAtIsNotNull(SERVICE_ORDER_ID)).thenReturn(true);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServiceOrderExecution result = startService.execute(VALID_ID);

            assertThat(result.getStatus()).isEqualTo(ServiceOrderExecutionStatus.IN_PROGRESS);
            verifyNoInteractions(outboxEventRepository);
        }

        @Test
        void shouldThrowServiceOrderExecutionNotFoundExceptionWhenNotFound() {
            when(repository.findById(VALID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> startService.execute(VALID_ID))
                    .isInstanceOf(ServiceOrderExecutionNotFoundException.class);

            verify(repository, never()).save(any());
            verifyNoInteractions(outboxEventRepository);
        }

        @Test
        void shouldThrowInvalidTransitionWhenExecutionIsNotPending() {
            when(repository.findById(VALID_ID)).thenReturn(Optional.of(createServiceOrderExecution(ServiceOrderExecutionStatus.IN_PROGRESS)));

            assertThatThrownBy(() -> startService.execute(VALID_ID))
                    .isInstanceOf(InvalidServiceOrderExecutionTransitionException.class);

            verify(repository, never()).save(any());
            verifyNoInteractions(outboxEventRepository);
        }
    }

    private ServiceOrderExecution createServiceOrderExecution(ServiceOrderExecutionStatus status) {
        return ServiceOrderExecution.reconstruct(
                VALID_ID,
                SERVICE_CATALOG_ID,
                SERVICE_ORDER_ID,
                status,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null
        );
    }
}