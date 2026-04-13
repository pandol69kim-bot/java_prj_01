package com.example.app.infrastructure.webclient;

import com.example.app.domain.externalsystem.ExternalSystem;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Map;

class ExternalApiClientTest {

    private MockWebServer mockWebServer;
    private ExternalApiClient client;
    private ExternalSystem testSystem;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .build();

        client = new ExternalApiClient(webClient);

        testSystem = ExternalSystem.of(
                "TEST", "테스트 시스템",
                mockWebServer.url("").toString().replaceAll("/$", ""),
                ExternalSystem.AuthType.API_KEY, "test-api-key"
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void GET_요청이_성공하면_응답을_반환한다() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"id\":\"1\",\"name\":\"test\"}")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // Act & Assert
        StepVerifier.create(client.get(testSystem, "/items/1", Map.class))
                .expectNextMatches(result -> "1".equals(result.get("id")))
                .verifyComplete();
    }

    @Test
    void 서버_5xx_오류_시_ExternalApiException을_발생시킨다() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\":\"Internal Server Error\"}")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // Act & Assert
        StepVerifier.create(client.get(testSystem, "/items/1", Map.class))
                .expectErrorMatches(e -> e instanceof ExternalApiException
                        && ((ExternalApiException) e).isServerError())
                .verify();
    }

    @Test
    void 클라이언트_4xx_오류_시_ExternalApiException을_발생시킨다() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("{\"error\":\"Not Found\"}")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // Act & Assert
        StepVerifier.create(client.get(testSystem, "/items/999", Map.class))
                .expectErrorMatches(e -> e instanceof ExternalApiException
                        && ((ExternalApiException) e).isClientError())
                .verify();
    }

    @Test
    void POST_요청이_성공하면_응답을_반환한다() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"id\":\"new-1\",\"status\":\"created\"}")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        Map<String, String> body = Map.of("name", "new-item");

        // Act & Assert
        StepVerifier.create(client.post(testSystem, "/items", body, Map.class))
                .expectNextMatches(result -> "new-1".equals(result.get("id")))
                .verifyComplete();
    }
}
