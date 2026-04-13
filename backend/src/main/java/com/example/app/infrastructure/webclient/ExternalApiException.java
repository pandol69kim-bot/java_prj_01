package com.example.app.infrastructure.webclient;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

/**
 * 외부 API 호출 중 발생하는 예외.
 * HTTP 상태 코드와 원본 응답 바디를 함께 보관한다.
 */
@Getter
public class ExternalApiException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final String responseBody;
    private final String systemCode;

    public ExternalApiException(String systemCode, HttpStatusCode statusCode, String responseBody) {
        super(String.format("[%s] 외부 API 오류 - status=%s, body=%s",
                systemCode, statusCode, responseBody));
        this.systemCode = systemCode;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public ExternalApiException(String systemCode, String message, Throwable cause) {
        super(String.format("[%s] 외부 API 연결 오류 - %s", systemCode, message), cause);
        this.systemCode = systemCode;
        this.statusCode = null;
        this.responseBody = null;
    }

    public boolean isClientError() {
        return statusCode != null && statusCode.is4xxClientError();
    }

    public boolean isServerError() {
        return statusCode != null && statusCode.is5xxServerError();
    }
}
