package com.cafepos.floor.api;

import com.cafepos.floor.application.FloorApplicationService;
import com.cafepos.floor.domain.DiningTable;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DiningTableControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FloorApplicationService floorApplicationService;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID floorId = UUID.randomUUID();
    private final UUID sourceTableId = UUID.randomUUID();
    private final UUID targetTableId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        DiningTableController controller = new DiningTableController(floorApplicationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        TenantContextHolder.setTenantId(cafeId.toString());

        UserPrincipal principal = new UserPrincipal(
                cafeId, UUID.randomUUID(), "cashier@test.com", "CASHIER"
        );
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CASHIER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities)
        );
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/v1/tables/{id}/merge should merge table and return 200")
    void mergeTablesEndpointSuccess() throws Exception {
        DiningTable mergedTable = new DiningTable(sourceTableId, cafeId, floorId, "T-1", 4, true);
        mergedTable.mergeInto(targetTableId);

        when(floorApplicationService.mergeTables(eq(cafeId), eq(sourceTableId), eq(targetTableId)))
                .thenReturn(mergedTable);

        DiningTableController.MergeTablesRequest request = new DiningTableController.MergeTablesRequest(targetTableId);

        mockMvc.perform(post("/api/v1/tables/{id}/merge", sourceTableId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sourceTableId.toString()))
                .andExpect(jsonPath("$.mergedIntoTableId").value(targetTableId.toString()));

        verify(floorApplicationService).mergeTables(eq(cafeId), eq(sourceTableId), eq(targetTableId));
    }

    @Test
    @DisplayName("POST /api/v1/tables/{id}/unmerge should unmerge table and return 200")
    void unmergeTableEndpointSuccess() throws Exception {
        DiningTable unmergedTable = new DiningTable(sourceTableId, cafeId, floorId, "T-1", 4, true);

        when(floorApplicationService.unmergeTable(eq(cafeId), eq(sourceTableId)))
                .thenReturn(unmergedTable);

        mockMvc.perform(post("/api/v1/tables/{id}/unmerge", sourceTableId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sourceTableId.toString()))
                .andExpect(jsonPath("$.mergedIntoTableId").doesNotExist());

        verify(floorApplicationService).unmergeTable(eq(cafeId), eq(sourceTableId));
    }

    @Test
    @DisplayName("GET /api/v1/tables/{id}/merged-children should return list of child merged tables")
    void getMergedChildrenEndpointSuccess() throws Exception {
        DiningTable childTable = new DiningTable(sourceTableId, cafeId, floorId, "T-1", 4, true);
        childTable.mergeInto(targetTableId);

        when(floorApplicationService.getMergedTablesForTarget(eq(cafeId), eq(targetTableId)))
                .thenReturn(List.of(childTable));

        mockMvc.perform(get("/api/v1/tables/{id}/merged-children", targetTableId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sourceTableId.toString()))
                .andExpect(jsonPath("$[0].mergedIntoTableId").value(targetTableId.toString()));

        verify(floorApplicationService).getMergedTablesForTarget(eq(cafeId), eq(targetTableId));
    }
}
