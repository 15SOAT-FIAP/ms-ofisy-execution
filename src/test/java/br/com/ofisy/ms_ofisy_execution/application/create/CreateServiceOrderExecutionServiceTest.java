package br.com.ofisy.ms_ofisy_execution.application.create;

import br.com.ofisy.ms_ofisy_execution.application.create.CreateServiceOrderExecutionUseCase.CreateServiceOrderExecutionCommand;
import br.com.ofisy.ms_ofisy_execution.domain.ProcessedEvent;
import br.com.ofisy.ms_ofisy_execution.domain.ProcessedEventRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateServiceOrderExecutionServiceTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final String EVENT_TYPE = "QuoteApproved";
    private static final UUID SERVICE_ORDER_ID = UUID.randomUUID();
    private static final UUID FIRST_CATALOG_ID = UUID.randomUUID();
    private static final UUID SECOND_CATALOG_ID = UUID.randomUUID();

    @Mock
    private ServiceOrderExecutionRepository repository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private CreateServiceOrderExecutionService createService;

    @Captor
    private ArgumentCaptor<ProcessedEvent> processedEventCaptor;

    @Captor
    private ArgumentCaptor<ServiceOrderExecution> executionCaptor;

    private CreateServiceOrderExecutionCommand command(List<UUID> serviceCatalogIds) {
        return new CreateServiceOrderExecutionCommand(EVENT_ID, EVENT_TYPE, SERVICE_ORDER_ID, serviceCatalogIds);
    }

    @Nested
    class Execute {

        @Test
        void shouldCreatePendingExecutionForEachServiceCatalog() {
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<ServiceOrderExecution> result = createService.execute(command(List.of(FIRST_CATALOG_ID, SECOND_CATALOG_ID)));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ServiceOrderExecution::getServiceCatalogId)
                    .containsExactly(FIRST_CATALOG_ID, SECOND_CATALOG_ID);
            assertThat(result).allSatisfy(execution -> {
                assertThat(execution.getServiceOrderId()).isEqualTo(SERVICE_ORDER_ID);
                assertThat(execution.getStatus()).isEqualTo(ServiceOrderExecutionStatus.PENDING);
            });
        }

        @Test
        void shouldRegisterEventAsProcessed() {
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            createService.execute(command(List.of(FIRST_CATALOG_ID)));

            verify(processedEventRepository).save(processedEventCaptor.capture());
            assertThat(processedEventCaptor.getValue().getEventId()).isEqualTo(EVENT_ID);
            assertThat(processedEventCaptor.getValue().getEventType()).isEqualTo(EVENT_TYPE);
            assertThat(processedEventCaptor.getValue().getProcessedAt()).isNotNull();
        }

        @Test
        void shouldIgnoreEventAlreadyProcessed() {
            when(processedEventRepository.existsById(EVENT_ID)).thenReturn(true);

            List<ServiceOrderExecution> result = createService.execute(command(List.of(FIRST_CATALOG_ID)));

            assertThat(result).isEmpty();
            verifyNoInteractions(repository);
            verify(processedEventRepository, never()).save(any());
        }

        @Test
        void shouldCreateOnlyOneExecutionWhenServiceCatalogIsRepeated() {
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<ServiceOrderExecution> result = createService.execute(command(List.of(FIRST_CATALOG_ID, FIRST_CATALOG_ID)));

            assertThat(result).hasSize(1);
            verify(repository, times(1)).save(any());
        }

        @Test
        void shouldSkipServiceCatalogThatAlreadyHasExecutionForServiceOrder() {
            when(repository.existsByServiceOrderIdAndServiceCatalogId(SERVICE_ORDER_ID, FIRST_CATALOG_ID)).thenReturn(true);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<ServiceOrderExecution> result = createService.execute(command(List.of(FIRST_CATALOG_ID, SECOND_CATALOG_ID)));

            verify(repository).save(executionCaptor.capture());
            assertThat(executionCaptor.getValue().getServiceCatalogId()).isEqualTo(SECOND_CATALOG_ID);
            assertThat(result).hasSize(1);
            verify(processedEventRepository).save(any());
        }

        @Test
        void shouldNotRegisterEventWhenSavingExecutionFails() {
            when(repository.save(any())).thenThrow(new RuntimeException("database unavailable"));

            assertThatThrownBy(() -> createService.execute(command(List.of(FIRST_CATALOG_ID))))
                    .isInstanceOf(RuntimeException.class);

            verify(processedEventRepository, never()).save(any());
        }

        @Test
        void shouldCheckExistingExecutionsForTheEventServiceOrder() {
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            createService.execute(command(List.of(FIRST_CATALOG_ID)));

            verify(repository).existsByServiceOrderIdAndServiceCatalogId(eq(SERVICE_ORDER_ID), eq(FIRST_CATALOG_ID));
        }
    }
}