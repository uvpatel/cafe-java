package com.cafepos.identity.api;

import com.cafepos.identity.application.AuthenticateUserUseCase;
import com.cafepos.identity.application.ChangePasswordCommand;
import com.cafepos.identity.application.LoginCommand;
import com.cafepos.identity.application.LoginResponse;
import com.cafepos.identity.application.RefreshTokenCommand;
import com.cafepos.identity.application.RefreshTokenResponse;
import com.cafepos.identity.application.UserProfileResponse;
import com.cafepos.identity.domain.EmployeeRole;
import com.cafepos.shared.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthenticateUserUseCase authenticateUserUseCase;

    private final UUID testCafeId = UUID.randomUUID();
    private final UUID testEmployeeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authenticateUserUseCase);
        UserPrincipal principal = new UserPrincipal(testCafeId, testEmployeeId, "test@cafepos.com", "OWNER");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return principal;
                    }
                })
                .build();
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

    @Test
    @DisplayName("GET /api/v1/auth/me should return 200 OK with current user profile")
    void shouldGetCurrentUserProfileSuccessfully() throws Exception {
        UserProfileResponse profileResponse = new UserProfileResponse(
                testEmployeeId,
                testCafeId,
                "Test User",
                "test@cafepos.com",
                EmployeeRole.OWNER,
                true,
                Instant.now()
        );

        when(authenticateUserUseCase.getCurrentUserProfile(testEmployeeId)).thenReturn(profileResponse);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(testEmployeeId.toString()))
                .andExpect(jsonPath("$.cafeId").value(testCafeId.toString()))
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@cafepos.com"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/auth/change-password should return 204 No Content on valid change")
    void shouldChangePasswordSuccessfully() throws Exception {
        ChangePasswordCommand command = new ChangePasswordCommand("oldPassword123", "newSecurePassword456");

        doNothing().when(authenticateUserUseCase).changePassword(eq(testEmployeeId), any(ChangePasswordCommand.class));

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isNoContent());
    }
}
