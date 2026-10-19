package br.com.ofisy.ms_ofisy_execution.adapters.gateways;

import br.com.ofisy.ms_ofisy_execution.TestcontainersConfiguration;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class JpaServiceOrderExecutionRepositoryIT {

    private static final String COMPLETED = ServiceOrderExecutionStatus.COMPLETED.name();

    private final UUID serviceCatalogId = UUID.randomUUID();

    @Autowired
    private JpaServiceOrderExecutionRepository jpa;

    @Nested
    @DisplayName("averageExecutionMinutesByServiceCatalogIdAndStatus")
    class AverageExecutionMinutes {

        @Test
        @DisplayName("Deve retornar zero quando não há execuções para o serviço")
        void shouldReturnZeroWhenNoExecutionsFound() {
            var result = jpa.averageExecutionMinutesByServiceCatalogIdAndStatus(serviceCatalogId, COMPLETED);

            assertThat(result).isZero();
        }

        @Test
        @DisplayName("Deve calcular a média em minutos das execuções concluídas")
        void shouldCalculateAverageInMinutesForCompletedExecutions() {
            save(ServiceOrderExecutionStatus.COMPLETED, at(10, 0), at(10, 30));
            save(ServiceOrderExecutionStatus.COMPLETED, at(9, 0), at(10, 0));

            var result = jpa.averageExecutionMinutesByServiceCatalogIdAndStatus(serviceCatalogId, COMPLETED);

            assertThat(result).isEqualTo(45.0);
        }

        @Test
        @DisplayName("Deve truncar os minutos de cada execução antes da média")
        void shouldTruncateMinutesOfEachExecution() {
            save(ServiceOrderExecutionStatus.COMPLETED, at(9, 0), at(9, 10).plusSeconds(59));
            save(ServiceOrderExecutionStatus.COMPLETED, at(9, 0), at(9, 15));

            var result = jpa.averageExecutionMinutesByServiceCatalogIdAndStatus(serviceCatalogId, COMPLETED);

            assertThat(result).isEqualTo(12.5);
        }

        @Test
        @DisplayName("Deve ignorar execuções canceladas mesmo com as duas datas preenchidas")
        void shouldIgnoreCancelledExecutions() {
            save(ServiceOrderExecutionStatus.CANCELLED, at(9, 0), at(9, 5));
            save(ServiceOrderExecutionStatus.COMPLETED, at(9, 0), at(9, 30));

            var result = jpa.averageExecutionMinutesByServiceCatalogIdAndStatus(serviceCatalogId, COMPLETED);

            assertThat(result).isEqualTo(30.0);
        }

        @Test
        @DisplayName("Deve ignorar execuções sem data de início ou de término")
        void shouldIgnoreExecutionsWithoutBothTimestamps() {
            save(ServiceOrderExecutionStatus.COMPLETED, null, at(10, 0));
            save(ServiceOrderExecutionStatus.COMPLETED, at(9, 0), null);
            save(ServiceOrderExecutionStatus.COMPLETED, at(9, 0), at(9, 20));

            var result = jpa.averageExecutionMinutesByServiceCatalogIdAndStatus(serviceCatalogId, COMPLETED);

            assertThat(result).isEqualTo(20.0);
        }

        @Test
        @DisplayName("Deve considerar zero minutos quando o término é anterior ao início")
        void shouldClampNegativeMinutesToZero() {
            save(ServiceOrderExecutionStatus.COMPLETED, at(10, 0), at(9, 0));
            save(ServiceOrderExecutionStatus.COMPLETED, at(9, 0), at(9, 40));

            var result = jpa.averageExecutionMinutesByServiceCatalogIdAndStatus(serviceCatalogId, COMPLETED);

            assertThat(result).isEqualTo(20.0);
        }

        @Test
        @DisplayName("Deve ignorar execuções de outros serviços do catálogo")
        void shouldIgnoreExecutionsFromOtherServiceCatalogs() {
            jpa.save(ServiceOrderExecutionMapper.toEntity(execution(UUID.randomUUID(),
                    ServiceOrderExecutionStatus.COMPLETED, at(9, 0), at(11, 0))));
            save(ServiceOrderExecutionStatus.COMPLETED, at(9, 0), at(9, 10));

            var result = jpa.averageExecutionMinutesByServiceCatalogIdAndStatus(serviceCatalogId, COMPLETED);

            assertThat(result).isEqualTo(10.0);
        }
    }

    private void save(ServiceOrderExecutionStatus status, LocalDateTime startedAt, LocalDateTime finishedAt) {
        jpa.save(ServiceOrderExecutionMapper.toEntity(execution(serviceCatalogId, status, startedAt, finishedAt)));
    }

    private ServiceOrderExecution execution(UUID catalogId, ServiceOrderExecutionStatus status,
                                            LocalDateTime startedAt, LocalDateTime finishedAt) {
        return ServiceOrderExecution.reconstruct(
                null, catalogId, UUID.randomUUID(),
                status, LocalDateTime.now(), LocalDateTime.now(),
                startedAt, finishedAt);
    }

    private LocalDateTime at(int hour, int minute) {
        return LocalDateTime.of(2026, 1, 1, hour, minute);
    }
}