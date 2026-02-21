package com.maplog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplog.common.storage.FileStorageService;
import com.maplog.user.command.dto.LoginRequest;
import com.maplog.user.command.dto.SignupRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FileStorageService fileStorageService;

    @Test
    @DisplayName("전체 회원 시나리오: 가입 -> 로그인 -> 일기 작성 -> 조회")
    void fullUserScenario() throws Exception {
        // 1. 회원가입
        SignupRequest signupRequest = new SignupRequest("integration@test.com", "password123", "인테기");
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        // 2. 로그인
        LoginRequest loginRequest = new LoginRequest("integration@test.com", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
        String accessToken = (String) ((Map<String, Object>) responseMap.get("data")).get("accessToken");

        // 3. 일기 작성 (Multipart)
        mockMvc.perform(multipart("/api/diaries")
                .param("title", "통합 테스트 일기")
                .param("content", "내용입니다")
                .param("latitude", "37.5")
                .param("longitude", "127.0")
                .param("locationName", "서울역")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated());

        // 4. 내 일기 목록 조회 (MyBatis Query Side)
        mockMvc.perform(get("/api/users/me/diaries")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("통합 테스트 일기"));
    }
}
