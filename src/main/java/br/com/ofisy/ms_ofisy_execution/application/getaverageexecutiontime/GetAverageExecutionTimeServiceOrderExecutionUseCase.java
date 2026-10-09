package br.com.ofisy.ms_ofisy_execution.application.getaverageexecutiontime;

import java.util.UUID;

public interface GetAverageExecutionTimeServiceOrderExecutionUseCase {

    double execute(UUID serviceCatalogId);
}
