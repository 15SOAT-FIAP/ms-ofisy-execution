package br.com.ofisy.ms_ofisy_execution.adapters.gateways;

import br.com.ofisy.ms_ofisy_execution.domain.OutboxEvent;
import br.com.ofisy.ms_ofisy_execution.domain.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final JpaOutboxEventRepository jpa;
    private final JsonMapper jsonMapper;

    @Override
    public void save(OutboxEvent outboxEvent) {
        String payloadJson = jsonMapper.writeValueAsString(outboxEvent.getPayload());
        jpa.save(OutboxEventMapper.toEntity(outboxEvent, payloadJson));
    }
}