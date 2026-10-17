package br.com.ofisy.ms_ofisy_execution.adapters.controllers;

import br.com.ofisy.ms_ofisy_execution.adapters.controllers.dto.ServiceOrderExecutionResponseDTO;
import br.com.ofisy.ms_ofisy_execution.adapters.presenters.ServiceOrderExecutionPresenter;
import br.com.ofisy.ms_ofisy_execution.application.complete.CompleteServiceOrderExecutionUseCase;
import br.com.ofisy.ms_ofisy_execution.application.start.StartServiceOrderExecutionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service_order_executions")
@RequiredArgsConstructor
public class ServiceOrderExecutionController {

    private final StartServiceOrderExecutionUseCase startServiceOrderExecutionUseCase;
    private final CompleteServiceOrderExecutionUseCase completeServiceOrderExecutionUseCase;

    @PatchMapping("/{id}/start")
    public ResponseEntity<ServiceOrderExecutionResponseDTO> start(@PathVariable UUID id) {
        return ResponseEntity.ok(ServiceOrderExecutionPresenter.present(startServiceOrderExecutionUseCase.execute(id)));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ServiceOrderExecutionResponseDTO> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(ServiceOrderExecutionPresenter.present(completeServiceOrderExecutionUseCase.execute(id)));
    }
}