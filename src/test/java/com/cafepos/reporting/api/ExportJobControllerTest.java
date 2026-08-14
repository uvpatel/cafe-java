package com.cafepos.reporting.api;

import com.cafepos.reporting.application.ReportingApplicationService;
import com.cafepos.reporting.domain.ExportJob;
import com.cafepos.reporting.domain.ExportStatus;
import com.cafepos.reporting.domain.ExportType;
import com.cafepos.shared.error.GlobalExceptionHandler;
import com.cafepos.shared.security.UserPrincipal;
import com.cafepos.shared.tenancy.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExportJobControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ReportingApplicationService reportingApplicationService;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ExportJobController controller = new ExportJobController(reportingApplicationService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        TenantContextHolder.setTenantId(cafeId.toString());
        UserPrincipal principal = new UserPrincipal(cafeId, employeeId, "owner@cafe.com", "OWNER");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/v1/exports should trigger export job with 202 ACCEPTED")
    void shouldTriggerExport() throws Exception {
        CreateExportRequest request = new CreateExportRequest(ExportType.SALES_SUMMARY, "CSV");
        ExportJob job = new ExportJob(jobId, cafeId, ExportType.SALES_SUMMARY, "CSV");

        when(reportingApplicationService.triggerExport(cafeId, ExportType.SALES_SUMMARY, "CSV")).thenReturn(job);

        mockMvc.perform(post("/api/v1/exports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(jobId.toString()))
                .andExpect(jsonPath("$.exportType").value("SALES_SUMMARY"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/exports/{id} should return export job status")
    void shouldGetExportStatus() throws Exception {
        ExportJob job = new ExportJob(jobId, cafeId, ExportType.SALES_SUMMARY, "CSV");

        when(reportingApplicationService.getExportJob(cafeId, jobId)).thenReturn(job);

        mockMvc.perform(get("/api/v1/exports/{id}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
