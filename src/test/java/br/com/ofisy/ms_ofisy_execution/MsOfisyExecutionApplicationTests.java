package br.com.ofisy.ms_ofisy_execution;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MsOfisyExecutionApplicationTests {

	@Test
	void contextLoads() {
	}

}
