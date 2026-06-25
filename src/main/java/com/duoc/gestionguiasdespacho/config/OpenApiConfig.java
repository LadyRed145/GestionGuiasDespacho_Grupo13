package com.duoc.gestionguiasdespacho.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI gestionGuiasOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("🚚🛡️📦 API Gestión de Guías de Despacho")
                        .version("1.0.0")
                        .description("""
                                Microservicio Cloud Native para la gestión de pedidos y generación de guías de despacho.

                                Funcionalidades principales:
                                - Crear guías de despacho.
                                - Generar documentos asociados a las guías.
                                - Subir guías generadas a AWS S3.
                                - Descargar guías con validación de permisos.
                                - Actualizar guías existentes.
                                - Eliminar guías específicas.
                                - Consultar guías por transportista y fecha.

                                El diseño visual usa camión, escudo y caja para representar:
                                - Transporte.
                                - Seguridad mediante JWT e IDaaS.
                                - Gestión documental y almacenamiento en S3.
                                """)
                        .contact(new Contact()
                                .name("Grupo 13 - Desarrollo Cloud Native")
                                .email("natalia.alvarado.cardozo.16@duocuc.cl"))
                        .license(new License()
                                .name("Uso académico - Duoc UC"))
                        .extensions(Map.of(
                                "x-logo", Map.of(
                                        "url", "/images/logo-gestion-guias.png",
                                        "altText", "Gestión de Guías de Despacho"
                                )
                        )))
                .externalDocs(new ExternalDocumentation()
                        .description("Repositorio y documentación técnica del proyecto")
                        .url("https://github.com/LadyRed145/gestionguiasdespacho"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Entorno local de desarrollo"),
                        new Server()
                                .url("https://api-gateway-url-pendiente")
                                .description("API Gateway AWS - pendiente de configuración")
                ))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT emitido por Azure AD B2C"))
                        .addSchemas("ErrorResponse",
                                new io.swagger.v3.oas.models.media.Schema<>()
                                        .type("object")
                                        .description("Respuesta estándar de error del microservicio")
                                        .addProperty("timestamp", new StringSchema()
                                                .description("Fecha y hora del error"))
                                        .addProperty("status", new StringSchema()
                                                .description("Código HTTP del error"))
                                        .addProperty("error", new StringSchema()
                                                .description("Descripción breve del error"))
                                        .addProperty("message", new StringSchema()
                                                .description("Mensaje detallado del error"))
                                        .addProperty("path", new StringSchema()
                                                .description("Endpoint donde ocurrió el error"))))
                .addSecurityItem(new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME));
    }
}
