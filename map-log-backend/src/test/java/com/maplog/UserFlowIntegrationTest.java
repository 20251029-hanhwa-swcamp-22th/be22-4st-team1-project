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
    @DisplayName("전체 시나리오: 가입 -> 로그인 -> 일기 작성(Private) -> 일기 작성(공유) -> 피드 조회")
    void fullScenario() throws Exception {
        // 1. 회원가입 및 로그인 (User A)
        signup("userA@test.com", "userA");
        String tokenA = login("userA@test.com");

        // 2. 회원가입 및 로그인 (User B)
        signup("userB@test.com", "userB");
        String tokenB = login("userB@test.com");
        
        // UserB의 ID 가져오기
        MvcResult meResult = mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();
        Long userIdB = objectMapper.readTree(meResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 3. User A가 Private 일기 작성
        mockMvc.perform(multipart("/api/diaries")
                .param("title", "A의 비밀 일기")
                .param("content", "비밀")
                .param("latitude", "37.5")
                .param("longitude", "127.0")
                .param("locationName", "집")
                .param("visibility", "PRIVATE")
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated());

        // 4. User A가 User B에게 공유하며 일기 작성
        mockMvc.perform(multipart("/api/diaries")
                .param("title", "B와 공유하는 일기")
                .param("content", "같이보자")
                .param("latitude", "37.6")
                .param("longitude", "127.1")
                .param("locationName", "카페")
                .param("visibility", "FRIENDS_ONLY")
                .param("sharedUserIds", userIdB.toString())
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated());

        // 5. User B가 피드 조회 (공유받은 일기만 보여야 함)
        mockMvc.perform(get("/api/diaries/feed")
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("B와 공유하는 일기"));

        // 6. User A가 내 일기 조회 (둘 다 보여야 함)
        mockMvc.perform(get("/api/users/me/diaries")
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    private void signup(String email, String nickname) throws Exception {
        SignupRequest request = new SignupRequest(email, "password123", nickname);
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        LoginRequest request = new LoginRequest(email, "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return (String) ((Map) objectMapper.readValue(body, Map.class).get("data")).get("accessToken");
    }
}