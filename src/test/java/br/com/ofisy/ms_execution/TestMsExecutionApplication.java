package br.com.ofisy.ms_execution;

import org.springframework.boot.SpringApplication;

public class TestMsExecutionApplication {

	public static void main(String[] args) {
		SpringApplication.from(MsExecutionApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
