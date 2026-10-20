package br.com.ofisy.ms_ofisy_execution.adapters.controllers;

import br.com.ofisy.ms_ofisy_execution.TestcontainersConfiguration;
import br.com.ofisy.ms_ofisy_execution.adapters.gateways.JpaOutboxEventRepository;
import br.com.ofisy.ms_ofisy_execution.adapters.gateways.OutboxEventEntity;
import br.com.ofisy.ms_ofisy_execution.application.complete.CompleteServiceOrderExecutionService;
import br.com.ofisy.ms_ofisy_execution.application.start.StartServiceOrderExecutionService;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServiceOrderExecutionControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ServiceOrderExecutionRepository executionRepository;

    @Autowired
    private JpaOutboxEventRepository outboxEventRepository;

    private UUID serviceOrderId;
    private UUID firstExecutionId;
    private UUID secondExecutionId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        serviceOrderId = UUID.randomUUID();
        firstExecutionId = executionRepository.save(ServiceOrderExecution.create(UUID.randomUUID(), serviceOrderId)).getId();
        secondExecutionId = executionRepository.save(ServiceOrderExecution.create(UUID.randomUUID(), serviceOrderId)).getId();
    }

    @Nested
    class Start {

        @Test
        void shouldRecordExecutionStartedOnlyWhenTheFirstExecutionOfTheServiceOrderStarts() {
            start(firstExecutionId)
                    .statusCode(200)
                    .body("id", equalTo(firstExecutionId.toString()))
                    .body("status", equalTo("IN_PROGRESS"))
                    .body("startedAt", notNullValue());

            assertThat(executionStartedEventsOf(serviceOrderId)).hasSize(1);

            start(secondExecutionId)
                    .statusCode(200)
                    .body("status", equalTo("IN_PROGRESS"));

            assertThat(executionStartedEventsOf(serviceOrderId)).hasSize(1);
        }

        @Test
        void shouldReturn409WhenExecutionIsAlreadyStarted() {
            start(firstExecutionId).statusCode(200);

            start(firstExecutionId)
                    .statusCode(409)
                    .body("title", equalTo("Transição de status inválida"));

            assertThat(executionStartedEventsOf(serviceOrderId)).hasSize(1);
        }

        @Test
        void shouldReturn404WhenExecutionDoesNotExist() {
            start(UUID.randomUUID())
                    .statusCode(404)
                    .body("title", equalTo("Execução não encontrada"));
        }
    }

    @Nested
    class Complete {

        @BeforeEach
        void startExecutions() {
            startDirectly(firstExecutionId);
            startDirectly(secondExecutionId);
        }

        @Test
        void shouldRecordAllExecutionsCompletedOnlyWhenTheLastExecutionOfTheServiceOrderCompletes() {
            complete(firstExecutionId)
                    .statusCode(200)
                    .body("id", equalTo(firstExecutionId.toString()))
                    .body("status", equalTo("COMPLETED"))
                    .body("finishedAt", notNullValue());

            assertThat(allExecutionsCompletedEventsOf(serviceOrderId)).isEmpty();

            complete(secondExecutionId)
                    .statusCode(200)
                    .body("status", equalTo("COMPLETED"));

            assertThat(allExecutionsCompletedEventsOf(serviceOrderId)).hasSize(1);
        }

        @Test
        void shouldRecordAllExecutionsCompletedWhenRemainingExecutionsAreCancelled() {
            ServiceOrderExecution second = executionRepository.findById(secondExecutionId).orElseThrow();
            second.cancel();
            executionRepository.save(second);

            complete(firstExecutionId)
                    .statusCode(200)
                    .body("status", equalTo("COMPLETED"));

            assertThat(allExecutionsCompletedEventsOf(serviceOrderId)).hasSize(1);
        }

        @Test
        void shouldReturn409WhenExecutionIsAlreadyCompleted() {
            complete(firstExecutionId).statusCode(200);

            complete(firstExecutionId)
                    .statusCode(409)
                    .body("title", equalTo("Transição de status inválida"));

            assertThat(allExecutionsCompletedEventsOf(serviceOrderId)).isEmpty();
        }

        @Test
        void shouldReturn409WhenExecutionIsPending() {
            UUID pendingExecutionId = executionRepository.save(ServiceOrderExecution.create(UUID.randomUUID(), UUID.randomUUID())).getId();

            complete(pendingExecutionId)
                    .statusCode(409)
                    .body("title", equalTo("Transição de status inválida"));
        }

        @Test
        void shouldReturn404WhenExecutionDoesNotExist() {
            complete(UUID.randomUUID())
                    .statusCode(404)
                    .body("title", equalTo("Execução não encontrada"));
        }

        @Test
        void shouldRecordASingleEventWhenTheLastExecutionsCompleteConcurrently() {
            CompletableFuture<Integer> first = CompletableFuture.supplyAsync(() -> complete(firstExecutionId).extract().statusCode());
            CompletableFuture<Integer> second = CompletableFuture.supplyAsync(() -> complete(secondExecutionId).extract().statusCode());

            assertThat(first.join()).isEqualTo(200);
            assertThat(second.join()).isEqualTo(200);
            assertThat(allExecutionsCompletedEventsOf(serviceOrderId)).hasSize(1);
        }
    }

    private void startDirectly(UUID executionId) {
        ServiceOrderExecution execution = executionRepository.findById(executionId).orElseThrow();
        execution.start();
        executionRepository.save(execution);
    }

    private ValidatableResponse start(UUID executionId) {
        return RestAssured.given()
                .when()
                .patch("/api/v1/service_order_executions/{id}/start", executionId)
                .then();
    }

    private ValidatableResponse complete(UUID executionId) {
        return RestAssured.given()
                .when()
                .patch("/api/v1/service_order_executions/{id}/complete", executionId)
                .then();
    }

    private List<OutboxEventEntity> executionStartedEventsOf(UUID serviceOrderId) {
        return eventsOf(serviceOrderId, StartServiceOrderExecutionService.EXECUTION_STARTED);
    }

    private List<OutboxEventEntity> allExecutionsCompletedEventsOf(UUID serviceOrderId) {
        return eventsOf(serviceOrderId, CompleteServiceOrderExecutionService.ALL_EXECUTIONS_COMPLETED);
    }

    private List<OutboxEventEntity> eventsOf(UUID serviceOrderId, String eventType) {
        return outboxEventRepository.findAll().stream()
                .filter(event -> event.getServiceOrderId().equals(serviceOrderId))
                .filter(event -> event.getEventType().equals(eventType))
                .toList();
    }
}