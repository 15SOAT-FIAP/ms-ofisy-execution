package br.com.ofisy.ms_ofisy_execution.adapters.controllers;

import br.com.ofisy.ms_ofisy_execution.TestcontainersConfiguration;
import br.com.ofisy.ms_ofisy_execution.adapters.gateways.JpaOutboxEventRepository;
import br.com.ofisy.ms_ofisy_execution.adapters.gateways.OutboxEventEntity;
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

    private ValidatableResponse start(UUID executionId) {
        return RestAssured.given()
                .when()
                .patch("/api/v1/service_order_executions/{id}/start", executionId)
                .then();
    }

    private List<OutboxEventEntity> executionStartedEventsOf(UUID serviceOrderId) {
        return outboxEventRepository.findAll().stream()
                .filter(event -> event.getServiceOrderId().equals(serviceOrderId))
                .filter(event -> event.getEventType().equals(StartServiceOrderExecutionService.EXECUTION_STARTED))
                .toList();
    }
}