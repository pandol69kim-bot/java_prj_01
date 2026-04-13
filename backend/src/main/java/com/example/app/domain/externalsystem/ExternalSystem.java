package com.example.app.domain.externalsystem;

import com.example.app.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 외부 시스템 연동 설정 정보를 관리하는 엔티티.
 * API 키, 엔드포인트, 인증 방식 등 연결 설정을 보관한다.
 */
@Entity
@Table(name = "external_systems", indexes = {
    @Index(name = "idx_external_systems_code", columnList = "system_code", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalSystem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_code", nullable = false, unique = true, length = 50)
    private String systemCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "api_key", length = 500)
    private String apiKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 20)
    private AuthType authType;

    @Column(name = "timeout_seconds", nullable = false)
    private int timeoutSeconds = 30;

    @Column(name = "retry_max_attempts", nullable = false)
    private int retryMaxAttempts = 3;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 500)
    private String description;

    public enum AuthType {
        NONE, API_KEY, BEARER_TOKEN, BASIC
    }

    public static ExternalSystem of(String systemCode, String name, String baseUrl,
                                     AuthType authType, String apiKey) {
        ExternalSystem es = new ExternalSystem();
        es.systemCode = systemCode;
        es.name = name;
        es.baseUrl = baseUrl;
        es.authType = authType;
        es.apiKey = apiKey;
        return es;
    }

    public ExternalSystem deactivate() {
        ExternalSystem updated = copyBase();
        updated.active = false;
        return updated;
    }

    public ExternalSystem updateApiKey(String newApiKey) {
        ExternalSystem updated = copyBase();
        updated.apiKey = newApiKey;
        return updated;
    }

    private ExternalSystem copyBase() {
        ExternalSystem es = new ExternalSystem();
        es.systemCode = this.systemCode;
        es.name = this.name;
        es.baseUrl = this.baseUrl;
        es.authType = this.authType;
        es.apiKey = this.apiKey;
        es.timeoutSeconds = this.timeoutSeconds;
        es.retryMaxAttempts = this.retryMaxAttempts;
        es.active = this.active;
        es.description = this.description;
        return es;
    }
}
