package com.cafepos.identity.api;

import com.cafepos.identity.application.ShiftApplicationService;
import com.cafepos.identity.domain.ShiftStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmployeeShiftControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShiftApplicationService shiftApplicationService;

    private UUID cafeId;
    private UUID employeeId;
    private UUID shiftId;
    private EmployeeShiftResponse sampleResponse;

    @BeforeEach
    void setUp() {
        EmployeeShiftController controller = new EmployeeShiftController(shiftApplicationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.cafepos.shared.error.GlobalExceptionHandler())
                .build();

        cafeId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        shiftId = UUID.randomUUID();

        sampleResponse = new EmployeeShiftResponse(
                shiftId, cafeId, employeeId, Instant.now(), null, null, null,
                ShiftStatus.ACTIVE, "Morning shift", 0L, Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/shifts/clock-in should start shift")
    void shouldClockIn() throws Exception {
        com.cafepos.shared.tenancy.TenantContextHolder.setTenantId(cafeId.toString());
        try {
            when(shiftApplicationService.clockIn(cafeId, employeeId, "Morning shift")).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/shifts/clock-in")
                            .param("employeeId", employeeId.toString())
                            .param("notes", "Morning shift"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(shiftId.toString()))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        } finally {
            com.cafepos.shared.tenancy.TenantContextHolder.clear();
        }
    }

    @Test
    @DisplayName("GET /api/v1/shifts/employees/{employeeId} should list shifts")
    void shouldGetEmployeeShifts() throws Exception {
        com.cafepos.shared.tenancy.TenantContextHolder.setTenantId(cafeId.toString());
        try {
            when(shiftApplicationService.getShiftsForEmployee(cafeId, employeeId)).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/v1/shifts/employees/{employeeId}", employeeId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(shiftId.toString()));
        } finally {
            com.cafepos.shared.tenancy.TenantContextHolder.clear();
        }
    }
}
