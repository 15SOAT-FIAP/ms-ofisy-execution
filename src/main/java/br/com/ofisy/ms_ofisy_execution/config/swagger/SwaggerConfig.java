package br.com.ofisy.ms_ofisy_execution.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ofisy Execution API")
                        .description("Microsservico de execucao dos servicos das ordens de servico da Ofisy")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("15SOAT - Grupo 81")));
    }
}