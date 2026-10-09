package br.com.ofisy.ms_ofisy_execution.application.cancel;

import br.com.ofisy.ms_ofisy_execution.application.exception.ServiceOrderExecutionNotFoundException;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelServiceOrderExecutionServiceTest {

    private static final UUID VALID_ID = UUID.randomUUID();

    @Mock
    private ServiceOrderExecutionRepository repository;

    @InjectMocks
    private CancelServiceOrderExecutionService cancelService;

    @Nested
    class Execute {

        @Test
        void shouldCancelServiceOrderExecutionSuccessfully() {
            var execution = createServiceOrderExecution(ServiceOrderExecutionStatus.PENDING);
            when(repository.findById(VALID_ID)).thenReturn(Optional.of(execution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServiceOrderExecution result = cancelService.execute(VALID_ID);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ServiceOrderExecutionStatus.CANCELLED);
            assertThat(result.getFinishedAt()).isNotNull();
            verify(repository).findById(VALID_ID);
            verify(repository).save(any());
        }

        @Test
        void shouldThrowServiceOrderExecutionNotFoundExceptionWhenNotFound() {
            when(repository.findById(VALID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cancelService.execute(VALID_ID))
                    .isInstanceOf(ServiceOrderExecutionNotFoundException.class);

            verify(repository).findById(VALID_ID);
        }
    }

    private ServiceOrderExecution createServiceOrderExecution(ServiceOrderExecutionStatus status) {
        return ServiceOrderExecution.reconstruct(
                VALID_ID,
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
