package com.example.animalpictures;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Title and description shown at the top of the Swagger UI (/swagger-ui.html). */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI animalPicturesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Snuggle Post · Animal Pictures API")
                        .version("v1")
                        .description("""
                                Fetches random pictures of cats, dogs and bears from public APIs, stores them \
                                in an embedded H2 database and serves them back.

                                Try it: open **POST /api/pictures/{animal}**, click **Try it out**, enter `dog` \
                                and a count, then **Execute**. Afterwards **GET /api/pictures/{animal}/latest** \
                                returns the last stored picture.

                                Errors use RFC 9457 problem details (`application/problem+json`)."""))
                .externalDocs(new ExternalDocumentation()
                        .description("Snuggle Post web app")
                        .url("/"));
    }
}
