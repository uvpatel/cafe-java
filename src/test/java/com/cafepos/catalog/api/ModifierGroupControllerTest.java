package com.cafepos.catalog.api;

import com.cafepos.catalog.application.CatalogModifierApplicationService;
import com.cafepos.catalog.domain.ModifierGroup;
import com.cafepos.catalog.domain.ModifierOption;
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

import java.math.BigDecimal;
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
class ModifierGroupControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CatalogModifierApplicationService catalogModifierApplicationService;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ModifierGroupController controller = new ModifierGroupController(catalogModifierApplicationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        TenantContextHolder.setTenantId(cafeId.toString());

        UserPrincipal principal = new UserPrincipal(cafeId, UUID.randomUUID(), "cashier@test.com", "CASHIER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CASHIER")))
        );
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/v1/catalog/modifiers/groups should create modifier group and return 201 CREATED")
    void createGroupEndpointSuccess() throws Exception {
        ModifierGroup group = new ModifierGroup(groupId, cafeId, "Milk Choice", 1, 1);

        when(catalogModifierApplicationService.createModifierGroup(eq(cafeId), eq("Milk Choice"), eq(1), eq(1)))
                .thenReturn(group);

        ModifierGroupController.CreateModifierGroupRequest request = new ModifierGroupController.CreateModifierGroupRequest("Milk Choice", 1, 1);

        mockMvc.perform(post("/api/v1/catalog/modifiers/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(groupId.toString()))
                .andExpect(jsonPath("$.name").value("Milk Choice"));

        verify(catalogModifierApplicationService).createModifierGroup(eq(cafeId), eq("Milk Choice"), eq(1), eq(1));
    }

    @Test
    @DisplayName("POST /api/v1/catalog/modifiers/groups/{groupId}/options should add option and return 201 CREATED")
    void addOptionEndpointSuccess() throws Exception {
        UUID optionId = UUID.randomUUID();
        ModifierOption option = new ModifierOption(optionId, groupId, "Oat Milk", new BigDecimal("0.7500"), true, 1);

        when(catalogModifierApplicationService.addOptionToGroup(eq(cafeId), eq(groupId), eq("Oat Milk"), eq(new BigDecimal("0.7500")), eq(true), eq(1)))
                .thenReturn(option);

        ModifierGroupController.CreateModifierOptionRequest request = new ModifierGroupController.CreateModifierOptionRequest("Oat Milk", new BigDecimal("0.7500"), true, 1);

        mockMvc.perform(post("/api/v1/catalog/modifiers/groups/{groupId}/options", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(optionId.toString()))
                .andExpect(jsonPath("$.name").value("Oat Milk"));

        verify(catalogModifierApplicationService).addOptionToGroup(eq(cafeId), eq(groupId), eq("Oat Milk"), eq(new BigDecimal("0.7500")), eq(true), eq(1));
    }

    @Test
    @DisplayName("GET /api/v1/catalog/modifiers/groups should return list of modifier groups")
    void getGroupsEndpointSuccess() throws Exception {
        ModifierGroup group = new ModifierGroup(groupId, cafeId, "Milk Choice", 1, 1);

        when(catalogModifierApplicationService.getModifierGroups(eq(cafeId))).thenReturn(List.of(group));

        mockMvc.perform(get("/api/v1/catalog/modifiers/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(groupId.toString()))
                .andExpect(jsonPath("$[0].name").value("Milk Choice"));

        verify(catalogModifierApplicationService).getModifierGroups(eq(cafeId));
    }

    @Test
    @DisplayName("POST /api/v1/catalog/modifiers/products/{productId}/groups/{groupId} should link group to product and return 204 NO_CONTENT")
    void linkGroupToProductEndpointSuccess() throws Exception {
        UUID productId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/catalog/modifiers/products/{productId}/groups/{groupId}", productId, groupId))
                .andExpect(status().isNoContent());

        verify(catalogModifierApplicationService).linkModifierGroupToProduct(eq(cafeId), eq(productId), eq(groupId));
    }

    @Test
    @DisplayName("DELETE /api/v1/catalog/modifiers/products/{productId}/groups/{groupId} should unlink group from product and return 204 NO_CONTENT")
    void unlinkGroupFromProductEndpointSuccess() throws Exception {
        UUID productId = UUID.randomUUID();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/catalog/modifiers/products/{productId}/groups/{groupId}", productId, groupId))
                .andExpect(status().isNoContent());

        verify(catalogModifierApplicationService).unlinkModifierGroupFromProduct(eq(cafeId), eq(productId), eq(groupId));
    }
}
