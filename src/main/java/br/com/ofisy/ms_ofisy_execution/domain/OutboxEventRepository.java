package br.com.ofisy.ms_ofisy_execution.domain;

public interface OutboxEventRepository {

    void save(OutboxEvent outboxEvent);
}