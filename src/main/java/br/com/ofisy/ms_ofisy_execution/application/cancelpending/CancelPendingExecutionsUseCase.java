package br.com.ofisy.ms_ofisy_execution.application.cancelpending;

import java.util.UUID;

public interface CancelPendingExecutionsUseCase {

    void execute(CancelPendingExecutionsCommand cmd);

    record CancelPendingExecutionsCommand(
            UUID eventId,
            String eventType,
            UUID serviceOrderId
    ) {}
}