package br.com.ofisy.ms_ofisy_execution.adapters.controllers;

import br.com.ofisy.ms_ofisy_execution.adapters.controllers.dto.ServiceOrderExecutionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "API de Serviços de Ordem de Serviço")
public interface ServiceOrderExecutionApi {

    @Operation(summary = "Listar serviços de ordem de serviço com paginação - Roles autorizadas: (ADMIN, ATTENDANT, MECHANIC)")
    @ApiResponse(responseCode = "200", description = "Serviços listados com sucesso")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    @GetMapping
    ResponseEntity<Page<ServiceOrderExecutionResponseDTO>> getAll(
            Pageable pageable);

    @Operation(summary = "Obter serviço de ordem de serviço por ID - Roles autorizadas: (ADMIN, ATTENDANT, MECHANIC)")
    @ApiResponse(responseCode = "200", description = "Serviço listado com sucesso")
    @ApiResponse(responseCode = "404", description = "Serviço não encontrado")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    @GetMapping("/{id}")
    ResponseEntity<ServiceOrderExecutionResponseDTO> getById(
            @PathVariable UUID id);

    @Operation(summary = "Listar serviços de ordem de serviço por ID do serviço - Roles autorizadas: (ADMIN, ATTENDANT, MECHANIC)")
    @ApiResponse(responseCode = "200", description = "Serviços listados com sucesso")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    @GetMapping(params = "serviceCatalogId")
    ResponseEntity<Page<ServiceOrderExecutionResponseDTO>> getByServiceCatalogId(
            @RequestParam UUID serviceCatalogId,
            Pageable pageable);

    @Operation(summary = "Listar serviços de ordem de serviço por status - Roles autorizadas: (ADMIN, ATTENDANT, MECHANIC)")
    @ApiResponse(responseCode = "200", description = "Serviços listados com sucesso")
    @ApiResponse(responseCode = "400", description = "Status inválido")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    @GetMapping(params = "status")
    ResponseEntity<Page<ServiceOrderExecutionResponseDTO>> getByStatus(
            @RequestParam String status,
            Pageable pageable);

    @Operation(summary = "Listar serviços de ordem de serviço por ID da ordem de serviço - Roles autorizadas: (ADMIN, ATTENDANT, MECHANIC)")
    @ApiResponse(responseCode = "200", description = "Serviços listados com sucesso")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    @GetMapping("/service_order/{id}")
    ResponseEntity<Page<ServiceOrderExecutionResponseDTO>> getByServiceOrderId(
            @PathVariable UUID id,
            Pageable pageable);

    @Operation(summary = "Obter tempo médio de execução de um serviço - Roles autorizadas: (ADMIN, ATTENDANT, MECHANIC)")
    @ApiResponse(responseCode = "200", description = "Tempo médio calculado com sucesso")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    @GetMapping("/execution_time_average/{serviceCatalogId}")
    ResponseEntity<Double> getExecutionTimeAverage(
            @PathVariable UUID serviceCatalogId);

    @Operation(summary = "Completar um serviço de ordem de serviço - Roles autorizadas: (ADMIN, ATTENDANT, MECHANIC)")
    @ApiResponse(responseCode = "200", description = "Serviço completado com sucesso")
    @ApiResponse(responseCode = "404", description = "Serviço não encontrado")
    @ApiResponse(responseCode = "409", description = "Serviço não está em andamento")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    @PatchMapping("/{id}/complete")
    ResponseEntity<ServiceOrderExecutionResponseDTO> complete(
            @PathVariable UUID id);

    @Operation(summary = "Iniciar um serviço de ordem de serviço - Roles autorizadas: (ADMIN, ATTENDANT, MECHANIC)")
    @ApiResponse(responseCode = "200", description = "Serviço iniciado com sucesso")
    @ApiResponse(responseCode = "404", description = "Serviço não encontrado")
    @ApiResponse(responseCode = "409", description = "Serviço não está pendente")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    @PatchMapping("/{id}/start")
    ResponseEntity<ServiceOrderExecutionResponseDTO> start(
            @PathVariable UUID id);
}