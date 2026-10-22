package br.com.ofisy.ms_ofisy_execution.adapters.controllers;

import br.com.ofisy.ms_ofisy_execution.application.complete.CompleteServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.exception.ServiceOrderExecutionNotFoundException;
import br.com.ofisy.ms_ofisy_execution.application.getaverageexecutiontime.GetAverageExecutionTimeServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.identifybyid.IdentifyByIdServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.list.ListServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.listbyservicecatalogid.ListByServiceCatalogIdServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.listbyserviceorderid.ListByServiceOrderIdServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.listbystatus.ListByStatusServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.start.StartServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecution;
import br.com.ofisy.ms_ofisy_execution.shared.jwt.JwtService;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import br.com.ofisy.ms_ofisy_execution.domain.exception.InvalidServiceOrderExecutionTransitionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServiceOrderExecutionController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "MECHANIC")
class ServiceOrderExecutionControllerTest {

    private static final String BASE_URL = "/api/v1/service_order_executions";
    private static final UUID ID = UUID.randomUUID();
    private static final UUID SERVICE_ORDER_ID = UUID.randomUUID();
    private static final UUID SERVICE_CATALOG_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private StartServiceOrderExecutionUseCase startServiceOrderExecutionUseCase;

    @MockitoBean
    private CompleteServiceOrderExecutionUseCase completeServiceOrderExecutionUseCase;

    @MockitoBean
    private ListServiceOrderExecutionUseCase listServiceOrderExecutionUseCase;

    @MockitoBean
    private IdentifyByIdServiceOrderExecutionUseCase identifyByIdServiceOrderExecutionUseCase;

    @MockitoBean
    private ListByServiceCatalogIdServiceOrderExecutionUseCase listByServiceCatalogIdUseCase;

    @MockitoBean
    private ListByStatusServiceOrderExecutionUseCase listByStatusUseCase;

    @MockitoBean
    private ListByServiceOrderIdServiceOrderExecutionUseCase listByServiceOrderIdUseCase;

    @MockitoBean
    private GetAverageExecutionTimeServiceOrderExecutionUseCase getAverageExecutionTimeUseCase;

    @Nested
    class Complete {

        @Test
        void shouldReturn200WithCompletedExecution() throws Exception {
            LocalDateTime startedAt = LocalDateTime.of(2026, 10, 17, 10, 0);
            LocalDateTime finishedAt = LocalDateTime.of(2026, 10, 17, 11, 0);
            ServiceOrderExecution execution = ServiceOrderExecution.reconstruct(
                    ID, SERVICE_CATALOG_ID, SERVICE_ORDER_ID, ServiceOrderExecutionStatus.COMPLETED, startedAt, finishedAt, startedAt, finishedAt);
            when(completeServiceOrderExecutionUseCase.execute(ID)).thenReturn(execution);

            mockMvc.perform(patch(BASE_URL + "/{id}/complete", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID.toString()))
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.finishedAt").value("2026-10-17T11:00:00"));
        }

        @Test
        void shouldReturn404WhenExecutionDoesNotExist() throws Exception {
            when(completeServiceOrderExecutionUseCase.execute(ID)).thenThrow(new ServiceOrderExecutionNotFoundException(ID));

            mockMvc.perform(patch(BASE_URL + "/{id}/complete", ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Execução não encontrada"));
        }

        @Test
        void shouldReturn409WhenTransitionIsInvalid() throws Exception {
            when(completeServiceOrderExecutionUseCase.execute(ID)).thenThrow(new InvalidServiceOrderExecutionTransitionException(
                    ServiceOrderExecutionStatus.PENDING, ServiceOrderExecutionStatus.COMPLETED));

            mockMvc.perform(patch(BASE_URL + "/{id}/complete", ID))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Transição de status inválida"));
        }
    }

    @Nested
    class Start {

        @Test
        void shouldReturn200WithStartedExecution() throws Exception {
            LocalDateTime now = LocalDateTime.of(2026, 10, 17, 10, 0);
            ServiceOrderExecution execution = ServiceOrderExecution.reconstruct(
                    ID, SERVICE_CATALOG_ID, SERVICE_ORDER_ID, ServiceOrderExecutionStatus.IN_PROGRESS, now, now, now, null);
            when(startServiceOrderExecutionUseCase.execute(ID)).thenReturn(execution);

            mockMvc.perform(patch(BASE_URL + "/{id}/start", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID.toString()))
                    .andExpect(jsonPath("$.serviceOrderId").value(SERVICE_ORDER_ID.toString()))
                    .andExpect(jsonPath("$.serviceCatalogId").value(SERVICE_CATALOG_ID.toString()))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.startedAt").value("2026-10-17T10:00:00"));
        }

        @Test
        void shouldReturn404WhenExecutionDoesNotExist() throws Exception {
            when(startServiceOrderExecutionUseCase.execute(ID)).thenThrow(new ServiceOrderExecutionNotFoundException(ID));

            mockMvc.perform(patch(BASE_URL + "/{id}/start", ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Execução não encontrada"))
                    .andExpect(jsonPath("$.detail").value("Execução de serviço não encontrada com ID: " + ID));
        }

        @Test
        void shouldReturn409WhenTransitionIsInvalid() throws Exception {
            when(startServiceOrderExecutionUseCase.execute(ID)).thenThrow(new InvalidServiceOrderExecutionTransitionException(
                    ServiceOrderExecutionStatus.COMPLETED, ServiceOrderExecutionStatus.IN_PROGRESS));

            mockMvc.perform(patch(BASE_URL + "/{id}/start", ID))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Transição de status inválida"));
        }

        @Test
        void shouldReturn400WhenIdIsNotUuid() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/{id}/start", "not-a-uuid"))
                    .andExpect(status().isBadRequest());
        }
    }
}