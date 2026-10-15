package com.example.githubsearcher.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI githubSearcherOpenApi() {
        return new OpenAPI().info(new Info()
                .title("GitHub Repository Searcher API")
                .version("1.0.0")
                .description("Search GitHub repositories, store them in PostgreSQL and query the stored results."));
    }
}
