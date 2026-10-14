package br.com.ofisy.ms_ofisy_execution.adapters.listeners;

import br.com.ofisy.ms_ofisy_execution.TestcontainersConfiguration;
import br.com.ofisy.ms_ofisy_execution.adapters.gateways.JpaProcessedEventRepository;
import br.com.ofisy.ms_ofisy_execution.adapters.gateways.JpaServiceOrderExecutionRepository;
import br.com.ofisy.ms_ofisy_execution.domain.ServiceOrderExecutionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ExecutionEventsListenerIT {

    @Autowired
    private SqsAsyncClient sqsAsyncClient;

    @Autowired
    private JpaServiceOrderExecutionRepository executionRepository;

    @Autowired
    private JpaProcessedEventRepository processedEventRepository;

    @Value("${ofisy.messaging.queues.execution}")
    private String queueName;

    @Test
    void shouldCreateExecutionsOnlyOnceWhenSameEventIsDeliveredTwice() {
        UUID eventId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        String body = quoteApprovedBody(eventId, serviceOrderId, UUID.randomUUID(), UUID.randomUUID());
        String queueUrl = queueUrl();

        // Dedup ids diferentes simulam uma reentrega fora da janela de deduplicação do FIFO
        send(queueUrl, serviceOrderId, body);
        send(queueUrl, serviceOrderId, body);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(pendingMessages(queueUrl)).isZero();
            assertThat(processedEventRepository.existsById(eventId)).isTrue();
        });
        assertThat(executionRepository.countByServiceOrderId(serviceOrderId)).isEqualTo(2);
        assertThat(executionRepository.countByServiceOrderIdAndStatus(serviceOrderId, ServiceOrderExecutionStatus.PENDING))
                .isEqualTo(2);
    }

    @Test
    void shouldAcknowledgeEventWithoutHandlerWithoutProcessingIt() {
        UUID eventId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        String body = """
                {
                  "eventId": "%s",
                  "type": "QuoteRejected",
                  "serviceOrderId": "%s",
                  "occurredAt": "2026-10-13T10:00:00Z",
                  "payload": { "quoteId": "%s" }
                }
                """.formatted(eventId, serviceOrderId, UUID.randomUUID());
        String queueUrl = queueUrl();

        send(queueUrl, serviceOrderId, body);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(pendingMessages(queueUrl)).isZero());
        assertThat(processedEventRepository.existsById(eventId)).isFalse();
        assertThat(executionRepository.countByServiceOrderId(serviceOrderId)).isZero();
    }

    private String quoteApprovedBody(UUID eventId, UUID serviceOrderId, UUID firstCatalogId, UUID secondCatalogId) {
        return """
                {
                  "eventId": "%s",
                  "type": "QuoteApproved",
                  "serviceOrderId": "%s",
                  "occurredAt": "2026-10-13T10:00:00Z",
                  "payload": {
                    "quoteId": "%s",
                    "serviceItems": [
                      { "serviceCatalogId": "%s" },
                      { "serviceCatalogId": "%s" }
                    ]
                  }
                }
                """.formatted(eventId, serviceOrderId, UUID.randomUUID(), firstCatalogId, secondCatalogId);
    }

    private String queueUrl() {
        return sqsAsyncClient.getQueueUrl(r -> r.queueName(queueName)).join().queueUrl();
    }

    private void send(String queueUrl, UUID serviceOrderId, String body) {
        sqsAsyncClient.sendMessage(r -> r
                .queueUrl(queueUrl)
                .messageGroupId(serviceOrderId.toString())
                .messageDeduplicationId(UUID.randomUUID().toString())
                .messageBody(body)).join();
    }

    private int pendingMessages(String queueUrl) {
        Map<QueueAttributeName, String> attributes = sqsAsyncClient.getQueueAttributes(r -> r
                .queueUrl(queueUrl)
                .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE))
                .join()
                .attributes();
        return attributes.values().stream().mapToInt(Integer::parseInt).sum();
    }
}