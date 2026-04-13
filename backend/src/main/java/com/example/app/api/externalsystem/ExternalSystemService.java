package com.example.app.api.externalsystem;

import com.example.app.common.exception.BusinessException;
import com.example.app.domain.externalsystem.ExternalSystem;
import com.example.app.domain.externalsystem.ExternalSystemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalSystemService {

    private final ExternalSystemRepository externalSystemRepository;

    @Transactional(readOnly = true)
    public Page<ExternalSystemResponse> findAll(Pageable pageable) {
        return externalSystemRepository.findAll(pageable)
                .map(ExternalSystemResponse::from);
    }

    @Transactional(readOnly = true)
    public List<ExternalSystemResponse> findAllActive() {
        return externalSystemRepository.findAllByActiveTrue().stream()
                .map(ExternalSystemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExternalSystemResponse findById(Long id) {
        return ExternalSystemResponse.from(getOrThrow(id));
    }

    @Transactional
    public ExternalSystemResponse create(ExternalSystemRequest request) {
        if (externalSystemRepository.existsBySystemCode(request.systemCode())) {
            throw BusinessException.badRequest(
                    "이미 존재하는 시스템 코드입니다: " + request.systemCode());
        }
        ExternalSystem saved = externalSystemRepository.save(
                ExternalSystem.of(
                        request.systemCode(), request.name(), request.baseUrl(),
                        request.authType(), request.apiKey()
                )
        );
        return ExternalSystemResponse.from(saved);
    }

    @Transactional
    public ExternalSystemResponse updateApiKey(Long id, String newApiKey) {
        ExternalSystem system = getOrThrow(id);
        ExternalSystem updated = externalSystemRepository.save(system.updateApiKey(newApiKey));
        return ExternalSystemResponse.from(updated);
    }

    @Transactional
    public void deactivate(Long id) {
        ExternalSystem system = getOrThrow(id);
        externalSystemRepository.save(system.deactivate());
    }

    private ExternalSystem getOrThrow(Long id) {
        return externalSystemRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(
                        "외부 시스템을 찾을 수 없습니다. id=" + id));
    }
}
