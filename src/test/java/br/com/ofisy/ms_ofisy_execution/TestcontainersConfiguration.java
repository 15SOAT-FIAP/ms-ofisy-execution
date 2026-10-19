package br.com.ofisy.ms_ofisy_execution;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
	}

	// Reaproveita o init script do compose para que a fila, a DLQ e a assinatura sejam as mesmas do ambiente local
	@Bean
	@ServiceConnection
	LocalStackContainer localStackContainer() {
		return new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.13.1"))
				.withServices("sns", "sqs")
				.withCopyFileToContainer(
						MountableFile.forHostPath("localstack/init/ready.d/01-messaging.sh", 0755),
						"/etc/localstack/init/ready.d/01-messaging.sh")
				.waitingFor(Wait.forHttp("/_localstack/init/ready")
						.forPort(4566)
						.forResponsePredicate(body -> body.contains("\"completed\": true")));
	}

}