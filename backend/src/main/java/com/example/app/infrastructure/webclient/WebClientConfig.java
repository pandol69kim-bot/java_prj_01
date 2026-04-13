package com.example.app.infrastructure.webclient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${external.api.timeout-seconds:30}")
    private int timeoutSeconds;

    /**
     * 외부 연동용 공통 WebClient Bean.
     * 커넥션 풀, 타임아웃, 로깅 필터를 기본 설정으로 포함한다.
     */
    @Bean
    public WebClient externalApiWebClient() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("external-api-pool")
                .maxConnections(100)
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(10))
                .pendingAcquireTimeout(Duration.ofSeconds(5))
                .pendingAcquireMaxCount(500)
                .evictInBackground(Duration.ofSeconds(60))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutSeconds * 1000)
                .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                )
                .compress(true);   // gzip 지원

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(requestLoggingFilter())
                .filter(responseLoggingFilter())
                .build();
    }

    private ExchangeFilterFunction requestLoggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug("[WebClient] 요청 → {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }

    private ExchangeFilterFunction responseLoggingFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.debug("[WebClient] 응답 ← HTTP {}", response.statusCode());
            return Mono.just(response);
        });
    }
}
