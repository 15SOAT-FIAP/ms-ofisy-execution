package br.com.ofisy.ms_ofisy_execution.domain;

import br.com.ofisy.ms_ofisy_execution.domain.exception.InvalidServiceOrderExecutionTransitionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class ServiceOrderExecutionTest {

    private UUID createRandomId() {
        return UUID.randomUUID();
    }

    private ServiceOrderExecution newExecution() {
        return ServiceOrderExecution.create(createRandomId(), createRandomId());
    }

    @Nested
    class CreateServiceOrderExecution {
        @Test
        void shouldCreateServiceOrderExecutionWithValidData() {
            var serviceCatalogId = createRandomId();
            var serviceOrderId = createRandomId();

            var result = ServiceOrderExecution.create(serviceCatalogId, serviceOrderId);

            assertThat(result).isNotNull();
            assertThat(result.getServiceCatalogId()).isEqualTo(serviceCatalogId);
            assertThat(result.getServiceOrderId()).isEqualTo(serviceOrderId);
        }

        @Test
        void shouldCreateServiceOrderExecutionWithPendingStatus() {
            var result = newExecution();
            assertThat(result.getStatus()).isEqualTo(ServiceOrderExecutionStatus.PENDING);
        }

        @Test
        void shouldSetCreatedAtAndUpdatedAt() {
            var result = newExecution();

            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    class ReconstructServiceOrderExecution {
        @Test
        void shouldRestoreAllFieldsFromPersistedData() {
            var id = createRandomId();
            var serviceCatalogId = createRandomId();
            var serviceOrderId = createRandomId();
            var createdAt = LocalDateTime.of(2026, 1, 10, 8, 0);
            var updatedAt = LocalDateTime.of(2026, 1, 10, 12, 0);
            var startedAt = LocalDateTime.of(2026, 1, 10, 9, 0);
            var finishedAt = LocalDateTime.of(2026, 1, 10, 12, 0);

            var result = ServiceOrderExecution.reconstruct(id, serviceCatalogId, serviceOrderId,
                    ServiceOrderExecutionStatus.COMPLETED, createdAt, updatedAt, startedAt, finishedAt);

            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getServiceCatalogId()).isEqualTo(serviceCatalogId);
            assertThat(result.getServiceOrderId()).isEqualTo(serviceOrderId);
            assertThat(result.getStatus()).isEqualTo(ServiceOrderExecutionStatus.COMPLETED);
            assertThat(result.getCreatedAt()).isEqualTo(createdAt);
            assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
            assertThat(result.getStartedAt()).isEqualTo(startedAt);
            assertThat(result.getFinishedAt()).isEqualTo(finishedAt);
        }
    }

    @Nested
    class StartServiceOrderExecution {
        @Test
        void shouldStartFromPending() {
            var service = newExecution();

            service.start();

            assertThat(service.getStatus()).isEqualTo(ServiceOrderExecutionStatus.IN_PROGRESS);
            assertThat(service.getStartedAt()).isNotNull();
        }

        @Test
        void shouldRejectStartWhenAlreadyInProgress() {
            var service = newExecution();
            service.start();

            assertThatThrownBy(service::start)
                    .isInstanceOf(InvalidServiceOrderExecutionTransitionException.class)
                    .hasMessageContaining("IN_PROGRESS");
        }

        @Test
        void shouldRejectStartWhenCompleted() {
            var service = newExecution();
            service.start();
            service.complete();

            assertThatThrownBy(service::start)
                    .isInstanceOf(InvalidServiceOrderExecutionTransitionException.class);
        }

        @Test
        void shouldRejectStartWhenCancelled() {
            var service = newExecution();
            service.cancel();

            assertThatThrownBy(service::start)
                    .isInstanceOf(InvalidServiceOrderExecutionTransitionException.class);
        }
    }

    @Nested
    class CompleteServiceOrderExecution {
        @Test
        void shouldCompleteFromInProgress() {
            var service = newExecution();
            service.start();

            service.complete();

            assertThat(service.getStatus()).isEqualTo(ServiceOrderExecutionStatus.COMPLETED);
            assertThat(service.getFinishedAt()).isNotNull();
        }

        @Test
        void shouldRejectCompleteFromPending() {
            var service = newExecution();

            assertThatThrownBy(service::complete)
                    .isInstanceOf(InvalidServiceOrderExecutionTransitionException.class)
                    .hasMessageContaining("PENDING")
                    .hasMessageContaining("COMPLETED");
        }

        @Test
        void shouldRejectCompleteWhenAlreadyCompleted() {
            var service = newExecution();
            service.start();
            service.complete();

            assertThatThrownBy(service::complete)
                    .isInstanceOf(InvalidServiceOrderExecutionTransitionException.class);
        }

        @Test
        void shouldRejectCompleteWhenCancelled() {
            var service = newExecution();
            service.cancel();

            assertThatThrownBy(service::complete)
                    .isInstanceOf(InvalidServiceOrderExecutionTransitionException.class);
        }
    }

    @Nested
    class CancelServiceOrderExecution {
        @Test
        void shouldCancelFromPending() {
            var service = newExecution();

            service.cancel();

            assertThat(service.getStatus()).isEqualTo(ServiceOrderExecutionStatus.CANCELLED);
            assertThat(service.getFinishedAt()).isNotNull();
        }

        @Test
        void shouldCancelFromInProgress() {
            var service = newExecution();
            service.start();

            service.cancel();

            assertThat(service.getStatus()).isEqualTo(ServiceOrderExecutionStatus.CANCELLED);
            assertThat(service.getFinishedAt()).isNotNull();
        }

        @Test
        void shouldRejectCancelWhenCompleted() {
            var service = newExecution();
            service.start();
            service.complete();

            assertThatThrownBy(service::cancel)
                    .isInstanceOf(InvalidServiceOrderExecutionTransitionException.class);
        }

        @Test
        void shouldRejectCancelWhenAlreadyCancelled() {
            var service = newExecution();
            service.cancel();

            assertThatThrownBy(service::cancel)
                    .isInstanceOf(InvalidServiceOrderExecutionTransitionException.class);
        }
    }

    @Nested
    class StatusTransitions {
        @Test
        void shouldFollowFullHappyPath() {
            var service = newExecution();
            assertThat(service.getStatus()).isEqualTo(ServiceOrderExecutionStatus.PENDING);

            service.start();
            assertThat(service.getStatus()).isEqualTo(ServiceOrderExecutionStatus.IN_PROGRESS);

            service.complete();
            assertThat(service.getStatus()).isEqualTo(ServiceOrderExecutionStatus.COMPLETED);
        }
    }
}
