package br.com.ofisy.ms_ofisy_execution;

import org.springframework.boot.SpringApplication;

public class TestMsOfisyExecutionApplication {

	public static void main(String[] args) {
		SpringApplication.from(MsOfisyExecutionApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
