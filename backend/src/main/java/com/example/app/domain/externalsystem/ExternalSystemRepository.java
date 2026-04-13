package com.example.app.domain.externalsystem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExternalSystemRepository extends JpaRepository<ExternalSystem, Long> {

    Optional<ExternalSystem> findBySystemCode(String systemCode);

    List<ExternalSystem> findAllByActiveTrue();

    boolean existsBySystemCode(String systemCode);
}
