package sfedu.ictis.woi.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class OpenApiConfig {

    // Константа, которая будет использоваться как ключ для Security Scheme
    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // 1. Добавляем информацию о вашем API (опционально, но полезно)
                .info(new Info()
                        .title("Мой API")
                        .version("1.0.0")
                        .description("Документация для моего Spring Boot приложения с JWT авторизацией"))
                // 2. Глобально применяем требование безопасности (Bearer токен)
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                // 3. Определяем саму схему безопасности (JWT Bearer)
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP) // Тип - HTTP аутентификация
                                        .scheme("bearer") // Схема - "bearer"
                                        .bearerFormat("JWT") // Формат токена - JWT
                        )
                );
    }

    @Bean
    public WebClient.Builder webClientBuilder() {

        HttpClient httpClient = HttpClient.create()
                // на установку соединения
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)

                // ответа от сервера
                .responseTimeout(Duration.ofSeconds(60))

                // на чтение/запись
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(60))
                        .addHandlerLast(new WriteTimeoutHandler(60)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}