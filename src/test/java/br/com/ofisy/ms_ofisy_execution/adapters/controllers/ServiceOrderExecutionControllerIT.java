package br.com.ofisy.ms_ofisy_execution.adapters.controllers;

import br.com.ofisy.ms_ofisy_execution.TestcontainersConfiguration;
import br.com.ofisy.ms_ofisy_execution.adapters.gateways.JpaOutboxEventRepository;
import br.com.ofisy.ms_ofisy_execution.adapters.gateways.OutboxEventEntity;
import br.com.ofisy.ms_ofisy_execution.application.complete.CompleteServiceOrderExecutionService;
import br.com.ofisy.ms_ofisy_execution.application.start.StartServiceOrderExecutionService;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServiceOrderExecutionControllerIT {

    private static final String BASE_URL = "/api/v1/service_order_executions";

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

    @Nested
    class GetAll {

        @Test
        void shouldReturnFirstPageWithDefaultSizeOf10() {
            RestAssured.given()
                    .when()
                    .get(BASE_URL)
                    .then()
                    .statusCode(200)
                    .body("content.size()", equalTo(10));
        }

        @Test
        void shouldRespectRequestedPageSize() {
            RestAssured.given()
                    .queryParam("size", 2)
                    .when()
                    .get(BASE_URL)
                    .then()
                    .statusCode(200)
                    .body("content.size()", equalTo(2));
        }
    }

    @Nested
    class GetById {

        @Test
        void shouldReturnExecution() {
            RestAssured.given()
                    .when()
                    .get(BASE_URL + "/{id}", firstExecutionId)
                    .then()
                    .statusCode(200)
                    .body("id", equalTo(firstExecutionId.toString()))
                    .body("serviceOrderId", equalTo(serviceOrderId.toString()))
                    .body("status", equalTo("PENDING"))
                    .body("createdAt", notNullValue());
        }

        @Test
        void shouldReturn404WhenExecutionDoesNotExist() {
            RestAssured.given()
                    .when()
                    .get(BASE_URL + "/{id}", UUID.randomUUID())
                    .then()
                    .statusCode(404)
                    .body("title", equalTo("Execução não encontrada"));
        }

        @Test
        void shouldReturn400WhenIdIsNotUuid() {
            RestAssured.given()
                    .when()
                    .get(BASE_URL + "/{id}", "not-a-uuid")
                    .then()
                    .statusCode(400);
        }
    }

    @Nested
    class GetByServiceOrderId {

        @Test
        void shouldReturnOnlyExecutionsOfTheServiceOrder() {
            RestAssured.given()
                    .when()
                    .get(BASE_URL + "/service_order/{id}", serviceOrderId)
                    .then()
                    .statusCode(200)
                    .body("content.size()", equalTo(2))
                    .body("content.id", containsInAnyOrder(firstExecutionId.toString(), secondExecutionId.toString()))
                    .body("content.serviceOrderId", everyItem(equalTo(serviceOrderId.toString())));
        }
    }

    @Nested
    class GetByServiceCatalogId {

        @Test
        void shouldReturnOnlyExecutionsOfTheServiceCatalog() {
            UUID serviceCatalogId = executionRepository.findById(firstExecutionId).orElseThrow().getServiceCatalogId();

            RestAssured.given()
                    .queryParam("serviceCatalogId", serviceCatalogId)
                    .when()
                    .get(BASE_URL)
                    .then()
                    .statusCode(200)
                    .body("content.size()", equalTo(1))
                    .body("content[0].id", equalTo(firstExecutionId.toString()))
                    .body("content[0].serviceCatalogId", equalTo(serviceCatalogId.toString()));
        }
    }

    @Nested
    class GetByStatus {

        @Test
        void shouldReturnOnlyExecutionsWithTheStatusIgnoringCase() {
            startDirectly(firstExecutionId);

            RestAssured.given()
                    .queryParam("status", "in_progress")
                    .queryParam("size", 100)
                    .when()
                    .get(BASE_URL)
                    .then()
                    .statusCode(200)
                    .body("content.status", everyItem(equalTo("IN_PROGRESS")))
                    .body("content.id", hasItem(firstExecutionId.toString()))
                    .body("content.id", not(hasItem(secondExecutionId.toString())));
        }

        @Test
        void shouldReturn400WhenStatusIsInvalid() {
            RestAssured.given()
                    .queryParam("status", "invalid")
                    .when()
                    .get(BASE_URL)
                    .then()
                    .statusCode(400)
                    .body("title", equalTo("Status inválido"))
                    .body("detail", equalTo("Status inválido: invalid"));
        }
    }

    @Nested
    class GetExecutionTimeAverage {

        @Test
        void shouldReturnAverageInMinutesOfTheFinishedExecutions() {
            UUID serviceCatalogId = UUID.randomUUID();
            LocalDateTime startedAt = LocalDateTime.of(2026, 10, 17, 10, 0);
            saveFinished(serviceCatalogId, startedAt, startedAt.plusMinutes(60));
            saveFinished(serviceCatalogId, startedAt, startedAt.plusMinutes(45));
            executionRepository.save(ServiceOrderExecution.create(serviceCatalogId, UUID.randomUUID()));

            RestAssured.given()
                    .when()
                    .get(BASE_URL + "/execution_time_average/{serviceCatalogId}", serviceCatalogId)
                    .then()
                    .statusCode(200)
                    .body(equalTo("52.5"));
        }

        @Test
        void shouldReturnZeroWhenServiceCatalogHasNoExecutions() {
            RestAssured.given()
                    .when()
                    .get(BASE_URL + "/execution_time_average/{serviceCatalogId}", UUID.randomUUID())
                    .then()
                    .statusCode(200)
                    .body(equalTo("0.0"));
        }

        private void saveFinished(UUID serviceCatalogId, LocalDateTime startedAt, LocalDateTime finishedAt) {
            executionRepository.save(ServiceOrderExecution.reconstruct(null, serviceCatalogId, UUID.randomUUID(),
                    ServiceOrderExecutionStatus.COMPLETED, startedAt, finishedAt, startedAt, finishedAt));
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