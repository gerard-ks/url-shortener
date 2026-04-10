package org.urlshortener.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.base-url}")
    private String baseUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url(baseUrl)
                                .description("URL Shortener API")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
                .schemaRequirement("Bearer Token", securityScheme());
    }

    private Info apiInfo() {
        return new Info()
                .title("URL Shortener API")
                .description("""
                        API RESTful de raccourcissement d'URL.
                        
                        Fonctionnalités :
                        - Création d'URLs courtes (avec code personnalisé optionnel)
                        - Redirection automatique vers l'URL originale
                        - Statistiques d'accès en temps réel (SSE)
                        - Gestion du cycle de vie (modification, suppression, expiration)
                        
                        Authentification :
                        - Token de propriété généré à la création (format: tok_xxx...)
                        - Requis uniquement pour modification et suppression
                        - Header: Authorization: Bearer tok_xxx...
                        """)
                .version("1.0.0")
                .license(new License()
                        .name("MIT")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("tok_xxx...")
                .description("Token de propriété fourni à la création de l'URL");
    }
}
