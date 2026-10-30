package br.com.ofisy.ms_ofisy_execution;

import br.com.ofisy.ms_ofisy_execution.config.jwt.JwtProperties;
import br.com.ofisy.ms_ofisy_execution.config.security.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({SecurityProperties.class, JwtProperties.class})
public class MsOfisyExecutionApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsOfisyExecutionApplication.class, args);
	}

}