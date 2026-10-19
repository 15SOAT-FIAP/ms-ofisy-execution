package br.com.ofisy.ms_ofisy_execution.application.getaverageexecutiontime;


import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAverageExecutionTimeServiceTest {

    private static final UUID SERVICE_CATALOG_ID = UUID.randomUUID();

    @Mock
    private ServiceOrderExecutionRepository repository;

    @InjectMocks
    private GetAverageExecutionTimeService service;

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("Deve retornar a média das execuções concluídas do catálogo")
        void shouldReturnAverageOfCompletedExecutions() {
            when(repository.averageExecutionMinutesByServiceCatalogIdAndStatus(
                    SERVICE_CATALOG_ID, ServiceOrderExecutionStatus.COMPLETED))
                    .thenReturn(45.0);

            var result = service.execute(SERVICE_CATALOG_ID);

            assertThat(result).isEqualTo(45.0);
        }
    }
}