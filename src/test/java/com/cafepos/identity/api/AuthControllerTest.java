package com.cafepos.identity.api;

import com.cafepos.identity.application.AuthenticateUserUseCase;
import com.cafepos.identity.application.LoginCommand;
import com.cafepos.identity.application.LoginResponse;
import com.cafepos.identity.application.RefreshTokenCommand;
import com.cafepos.identity.application.RefreshTokenResponse;
import com.cafepos.identity.domain.EmployeeRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthenticateUserUseCase authenticateUserUseCase;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authenticateUserUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200 OK with login tokens")
    void shouldLoginSuccessfully() throws Exception {
        UUID cafeId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LoginCommand command = new LoginCommand(cafeId, "test@cafepos.com", "password123");
        LoginResponse response = new LoginResponse(
                "access.jwt.token",
                "refresh.jwt.token",
                "Bearer",
                900L,
                employeeId,
                cafeId,
                "test@cafepos.com",
                EmployeeRole.OWNER
        );

        when(authenticateUserUseCase.authenticate(any(LoginCommand.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.jwt.token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.cafeId").value(cafeId.toString()))
                .andExpect(jsonPath("$.email").value("test@cafepos.com"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh should return 200 OK with refreshed tokens")
    void shouldRefreshSuccessfully() throws Exception {
        RefreshTokenCommand command = new RefreshTokenCommand("valid.refresh.token");
        RefreshTokenResponse response = new RefreshTokenResponse(
                "new.access.jwt.token",
                "new.refresh.jwt.token",
                "Bearer",
                900L
        );

        when(authenticateUserUseCase.refreshToken(any(RefreshTokenCommand.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.jwt.token"))
                .andExpect(jsonPath("$.refreshToken").value("new.refresh.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }
}
