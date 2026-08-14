package com.cafepos.activitylog.api;

import com.cafepos.activitylog.application.ActivityLogApplicationService;
import com.cafepos.activitylog.domain.ActivityLog;
import com.cafepos.shared.error.GlobalExceptionHandler;
import com.cafepos.shared.security.UserPrincipal;
import com.cafepos.shared.tenancy.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ActivityLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ActivityLogApplicationService activityLogApplicationService;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ActivityLogController controller = new ActivityLogController(activityLogApplicationService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        TenantContextHolder.setTenantId(cafeId.toString());
        UserPrincipal principal = new UserPrincipal(cafeId, employeeId, "auditor@cafe.com", "AUDITOR");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_AUDITOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/audit-logs should return page of activity logs")
    void searchAuditLogsShouldReturnPagedLogs() throws Exception {
        UUID logId = UUID.randomUUID();
        ActivityLog log = new ActivityLog(
                logId,
                cafeId,
                employeeId,
                "auditor@cafe.com",
                "PAYMENT_PROCESSED",
                "PAYMENT",
                UUID.randomUUID().toString(),
                "{\"amount\":100.0}",
                "127.0.0.1"
        );
        Page<ActivityLog> page = new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1);

        when(activityLogApplicationService.searchActivityLogs(eq(cafeId), nullable(String.class), nullable(String.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(logId.toString()))
                .andExpect(jsonPath("$.content[0].cafeId").value(cafeId.toString()))
                .andExpect(jsonPath("$.content[0].action").value("PAYMENT_PROCESSED"))
                .andExpect(jsonPath("$.content[0].entityType").value("PAYMENT"));
    }

    @Test
    @DisplayName("GET /api/v1/audit-logs with action filter should query application service")
    void searchAuditLogsWithFilterShouldReturnFilteredLogs() throws Exception {
        UUID logId = UUID.randomUUID();
        ActivityLog log = new ActivityLog(
                logId,
                cafeId,
                employeeId,
                "admin@cafe.com",
                "USER_ONBOARDED",
                "EMPLOYEE",
                UUID.randomUUID().toString(),
                "{}",
                "127.0.0.1"
        );
        Page<ActivityLog> page = new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1);

        when(activityLogApplicationService.searchActivityLogs(eq(cafeId), eq("USER_ONBOARDED"), eq("EMPLOYEE"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("action", "USER_ONBOARDED")
                        .param("entityType", "EMPLOYEE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("USER_ONBOARDED"))
                .andExpect(jsonPath("$.content[0].entityType").value("EMPLOYEE"));
    }
}
