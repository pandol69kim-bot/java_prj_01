package com.example.app.infrastructure.webclient;

import com.example.app.domain.externalsystem.ExternalSystem;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 외부 시스템 HTTP 통신을 담당하는 공통 클라이언트.
 * Circuit Breaker와 Retry를 조합해 장애 전파를 차단한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalApiClient {

    private final WebClient externalApiWebClient;

    @CircuitBreaker(name = "externalApi", fallbackMethod = "getFallback")
    @Retry(name = "externalApi")
    public <T> Mono<T> get(ExternalSystem system, String path,
                            Class<T> responseType) {
        return externalApiWebClient.get()
                .uri(system.getBaseUrl() + path)
                .headers(h -> applyAuth(h, system))
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new ExternalApiException(system.getSystemCode(),
                                                response.statusCode(), body))))
                .onStatus(status -> status.is5xxServerError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new ExternalApiException(system.getSystemCode(),
                                                response.statusCode(), body))))
                .bodyToMono(responseType)
                .doOnError(ExternalApiException.class, e ->
                        log.warn("[{}] GET {} 실패: {}", system.getSystemCode(), path, e.getMessage()))
                .doOnError(WebClientResponseException.class, e ->
                        log.warn("[{}] GET {} 네트워크 오류: {}", system.getSystemCode(), path, e.getMessage()));
    }

    @CircuitBreaker(name = "externalApi", fallbackMethod = "postFallback")
    @Retry(name = "externalApi")
    public <T> Mono<T> post(ExternalSystem system, String path,
                             Object requestBody, Class<T> responseType) {
        return externalApiWebClient.post()
                .uri(system.getBaseUrl() + path)
                .headers(h -> applyAuth(h, system))
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new ExternalApiException(system.getSystemCode(),
                                                response.statusCode(), body))))
                .onStatus(status -> status.is5xxServerError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new ExternalApiException(system.getSystemCode(),
                                                response.statusCode(), body))))
                .bodyToMono(responseType)
                .doOnError(e -> log.warn("[{}] POST {} 실패: {}",
                        system.getSystemCode(), path, e.getMessage()));
    }

    @CircuitBreaker(name = "externalApi", fallbackMethod = "listFallback")
    @Retry(name = "externalApi")
    public <T> Flux<T> getList(ExternalSystem system, String path,
                                Class<T> elementType) {
        return externalApiWebClient.get()
                .uri(system.getBaseUrl() + path)
                .headers(h -> applyAuth(h, system))
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new ExternalApiException(system.getSystemCode(),
                                                response.statusCode(), body))))
                .onStatus(status -> status.is5xxServerError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new ExternalApiException(system.getSystemCode(),
                                                response.statusCode(), body))))
                .bodyToFlux(elementType)
                .doOnError(e -> log.warn("[{}] GET(list) {} 실패: {}",
                        system.getSystemCode(), path, e.getMessage()));
    }

    // ── 인증 헤더 적용 ────────────────────────────────────────────────────────

    private void applyAuth(HttpHeaders headers, ExternalSystem system) {
        switch (system.getAuthType()) {
            case API_KEY -> headers.set("X-Api-Key", system.getApiKey());
            case BEARER_TOKEN -> headers.setBearerAuth(system.getApiKey());
            case BASIC -> headers.setBasicAuth(system.getApiKey());
            case NONE -> { /* 인증 없음 */ }
        }
    }

    // ── Fallback 메서드 (Circuit Breaker 개방 시 호출) ────────────────────────

    public <T> Mono<T> getFallback(ExternalSystem system, String path,
                                    Class<T> responseType, Throwable t) {
        log.error("[{}] Circuit Breaker 개방 — GET {} 차단됨: {}",
                system.getSystemCode(), path, t.getMessage());
        return Mono.error(new ExternalApiException(system.getSystemCode(),
                "서킷 브레이커가 열려 있습니다. 잠시 후 다시 시도해주세요.", t));
    }

    public <T> Mono<T> postFallback(ExternalSystem system, String path,
                                     Object requestBody, Class<T> responseType, Throwable t) {
        log.error("[{}] Circuit Breaker 개방 — POST {} 차단됨: {}",
                system.getSystemCode(), path, t.getMessage());
        return Mono.error(new ExternalApiException(system.getSystemCode(),
                "서킷 브레이커가 열려 있습니다. 잠시 후 다시 시도해주세요.", t));
    }

    public <T> Flux<T> listFallback(ExternalSystem system, String path,
                                     Class<T> elementType, Throwable t) {
        log.error("[{}] Circuit Breaker 개방 — GET(list) {} 차단됨: {}",
                system.getSystemCode(), path, t.getMessage());
        return Flux.error(new ExternalApiException(system.getSystemCode(),
                "서킷 브레이커가 열려 있습니다. 잠시 후 다시 시도해주세요.", t));
    }
}
