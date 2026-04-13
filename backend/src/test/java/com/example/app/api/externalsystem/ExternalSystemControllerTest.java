package com.example.app.api.externalsystem;

import com.example.app.common.dto.PageResponse;
import com.example.app.domain.externalsystem.ExternalSystem;
import com.example.app.infrastructure.security.JwtTokenProvider;
import com.example.app.infrastructure.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExternalSystemController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ExternalSystemControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ExternalSystemService externalSystemService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean com.example.app.infrastructure.security.CustomUserDetailsService customUserDetailsService;
    @MockBean org.springframework.security.authentication.AuthenticationManager authenticationManager;

    private ExternalSystemResponse sampleResponse() {
        return new ExternalSystemResponse(1L, "ERP", "ERP 시스템",
                "https://api.erp.com", "API_KEY", 30, 3, true,
                "설명", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @WithMockUser(roles = "USER")
    void 목록_조회_성공() throws Exception {
        given(externalSystemService.findAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/external-systems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].systemCode").value("ERP"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void 단건_조회_성공() throws Exception {
        given(externalSystemService.findById(1L)).willReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/external-systems/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.systemCode").value("ERP"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ADMIN이_시스템을_등록한다() throws Exception {
        ExternalSystemRequest request = new ExternalSystemRequest(
                "NEW_SYS", "새 시스템", "https://api.new.com",
                ExternalSystem.AuthType.API_KEY, "key-123", 30, 3, null);

        given(externalSystemService.create(any())).willReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/external-systems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void 일반_사용자는_시스템_등록_불가() throws Exception {
        ExternalSystemRequest request = new ExternalSystemRequest(
                "NEW_SYS", "새 시스템", "https://api.new.com",
                ExternalSystem.AuthType.NONE, null, 30, 3, null);

        mockMvc.perform(post("/api/v1/external-systems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ADMIN이_시스템을_비활성화한다() throws Exception {
        willDoNothing().given(externalSystemService).deactivate(1L);

        mockMvc.perform(delete("/api/v1/external-systems/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void 잘못된_systemCode_형식은_400을_반환한다() throws Exception {
        ExternalSystemRequest invalid = new ExternalSystemRequest(
                "invalid code!", "이름", "https://api.com",
                ExternalSystem.AuthType.NONE, null, 30, 3, null);

        mockMvc.perform(post("/api/v1/external-systems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }
}
