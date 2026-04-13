package com.example.app.api.syncdata;

import com.example.app.infrastructure.security.JwtTokenProvider;
import com.example.app.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SyncDataController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class SyncDataControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean SyncDataQueryService syncDataQueryService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean com.example.app.infrastructure.security.CustomUserDetailsService customUserDetailsService;
    @MockBean org.springframework.security.authentication.AuthenticationManager authenticationManager;

    private SyncDataResponse sample() {
        return new SyncDataResponse(1L, "ERP", "REF-001", "COMPLETED",
                "{}", null, 0, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @WithMockUser(roles = "USER")
    void 시스템별_동기화_데이터_목록_조회() throws Exception {
        given(syncDataQueryService.findBySystem(eq(1L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(sample())));

        mockMvc.perform(get("/api/v1/sync-data").param("systemId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].externalRefId").value("REF-001"))
                .andExpect(jsonPath("$.meta.total").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void 단건_조회_성공() throws Exception {
        given(syncDataQueryService.findById(1L)).willReturn(sample());

        mockMvc.perform(get("/api/v1/sync-data/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void 인증_없이_접근하면_401() throws Exception {
        mockMvc.perform(get("/api/v1/sync-data").param("systemId", "1"))
                .andExpect(status().isUnauthorized());
    }
}
