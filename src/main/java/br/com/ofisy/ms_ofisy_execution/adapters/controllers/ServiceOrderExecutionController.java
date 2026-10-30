package br.com.ofisy.ms_ofisy_execution.adapters.controllers;

import br.com.ofisy.ms_ofisy_execution.adapters.controllers.dto.ServiceOrderExecutionResponseDTO;
import br.com.ofisy.ms_ofisy_execution.adapters.presenters.ServiceOrderExecutionPresenter;
import br.com.ofisy.ms_ofisy_execution.application.complete.CompleteServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.getaverageexecutiontime.GetAverageExecutionTimeServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.identifybyid.IdentifyByIdServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.list.ListServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.listbyservicecatalogid.ListByServiceCatalogIdServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.listbyserviceorderid.ListByServiceOrderIdServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.listbystatus.ListByStatusServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.start.StartServiceOrderExecutionUseCase;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service_order_executions")
@RequiredArgsConstructor
public class ServiceOrderExecutionController implements ServiceOrderExecutionApi {

    private final ListServiceOrderExecutionUseCase listServiceOrderExecutionUseCase;
    private final IdentifyByIdServiceOrderExecutionUseCase identifyByIdServiceOrderExecutionUseCase;
    private final ListByServiceCatalogIdServiceOrderExecutionUseCase listByServiceCatalogIdUseCase;
    private final ListByStatusServiceOrderExecutionUseCase listByStatusUseCase;
    private final ListByServiceOrderIdServiceOrderExecutionUseCase listByServiceOrderIdUseCase;
    private final GetAverageExecutionTimeServiceOrderExecutionUseCase getAverageExecutionTimeUseCase;
    private final StartServiceOrderExecutionUseCase startServiceOrderExecutionUseCase;
    private final CompleteServiceOrderExecutionUseCase completeServiceOrderExecutionUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT', 'MECHANIC')")
    public ResponseEntity<Page<ServiceOrderExecutionResponseDTO>> getAll(
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(listServiceOrderExecutionUseCase.execute(pageable).map(ServiceOrderExecutionPresenter::present));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT', 'MECHANIC')")
    public ResponseEntity<ServiceOrderExecutionResponseDTO> getById(@PathVariable UUID id) {

        return ResponseEntity.ok(ServiceOrderExecutionPresenter.present(identifyByIdServiceOrderExecutionUseCase.execute(id)));
    }

    @GetMapping(params = "serviceCatalogId")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT', 'MECHANIC')")
    public ResponseEntity<Page<ServiceOrderExecutionResponseDTO>> getByServiceCatalogId(
            @RequestParam UUID serviceCatalogId,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(listByServiceCatalogIdUseCase.execute(serviceCatalogId, pageable).map(ServiceOrderExecutionPresenter::present));
    }

    @GetMapping(params = "status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT', 'MECHANIC')")
    public ResponseEntity<Page<ServiceOrderExecutionResponseDTO>> getByStatus(
            @RequestParam String status,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(listByStatusUseCase.execute(status, pageable).map(ServiceOrderExecutionPresenter::present));
    }

    @GetMapping("/service_order/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT', 'MECHANIC')")
    public ResponseEntity<Page<ServiceOrderExecutionResponseDTO>> getByServiceOrderId(
            @PathVariable UUID id,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(listByServiceOrderIdUseCase.execute(id, pageable).map(ServiceOrderExecutionPresenter::present));
    }

    @GetMapping("/execution_time_average/{serviceCatalogId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT', 'MECHANIC')")
    public ResponseEntity<Double> getExecutionTimeAverage(@PathVariable UUID serviceCatalogId) {

        return ResponseEntity.ok(getAverageExecutionTimeUseCase.execute(serviceCatalogId));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT', 'MECHANIC')")
    public ResponseEntity<ServiceOrderExecutionResponseDTO> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(ServiceOrderExecutionPresenter.present(completeServiceOrderExecutionUseCase.execute(id)));
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT', 'MECHANIC')")
    public ResponseEntity<ServiceOrderExecutionResponseDTO> start(@PathVariable UUID id) {
        return ResponseEntity.ok(ServiceOrderExecutionPresenter.present(startServiceOrderExecutionUseCase.execute(id)));
    }
}