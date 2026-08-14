package com.cafepos.catalog.api;

import com.cafepos.catalog.application.ManageCategoryUseCase;
import com.cafepos.shared.error.BusinessException;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductCategoryControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ManageCategoryUseCase manageCategoryUseCase;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ProductCategoryController controller = new ProductCategoryController(manageCategoryUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.cafepos.shared.error.GlobalExceptionHandler())
                .build();

        TenantContextHolder.setTenantId(cafeId.toString());
        UserPrincipal principal = new UserPrincipal(cafeId, UUID.randomUUID(), "admin@cafe.com", "ADMIN");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/categories should return list of categories")
    void shouldGetCategories() throws Exception {
        CategoryResponse response = new CategoryResponse(
                categoryId, cafeId, "Beverages", "#FF0000", 1, false, Instant.now(), Instant.now()
        );
        when(manageCategoryUseCase.getCategoriesByCafe(cafeId, false)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(categoryId.toString()))
                .andExpect(jsonPath("$[0].name").value("Beverages"))
                .andExpect(jsonPath("$[0].colorHex").value("#FF0000"))
                .andExpect(jsonPath("$[0].sortOrder").value(1))
                .andExpect(jsonPath("$[0].archived").value(false));

        verify(manageCategoryUseCase).getCategoriesByCafe(cafeId, false);
    }

    @Test
    @DisplayName("GET /api/v1/categories?includeArchived=true should include archived categories")
    void shouldGetCategoriesIncludingArchived() throws Exception {
        CategoryResponse response = new CategoryResponse(
                categoryId, cafeId, "Beverages", "#FF0000", 1, true, Instant.now(), Instant.now()
        );
        when(manageCategoryUseCase.getCategoriesByCafe(cafeId, true)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/categories?includeArchived=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(categoryId.toString()))
                .andExpect(jsonPath("$[0].archived").value(true));

        verify(manageCategoryUseCase).getCategoriesByCafe(cafeId, true);
    }

    @Test
    @DisplayName("POST /api/v1/categories should create and return category with 201 CREATED")
    void shouldCreateCategory() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Desserts", "#00FF00", 2);
        CategoryResponse response = new CategoryResponse(
                categoryId, cafeId, "Desserts", "#00FF00", 2, false, Instant.now(), Instant.now()
        );

        when(manageCategoryUseCase.createCategory(eq(cafeId), any(CreateCategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.name").value("Desserts"))
                .andExpect(jsonPath("$.colorHex").value("#00FF00"))
                .andExpect(jsonPath("$.sortOrder").value(2));

        verify(manageCategoryUseCase).createCategory(eq(cafeId), any(CreateCategoryRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/categories with invalid payload should return 400 Bad Request")
    void shouldReturn400ForInvalidCreateCategory() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("", "invalid-color", -1);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/categories/{id} should update and return category")
    void shouldUpdateCategory() throws Exception {
        UpdateCategoryRequest request = new UpdateCategoryRequest("Hot Beverages", "#FF0000", 1);
        CategoryResponse response = new CategoryResponse(
                categoryId, cafeId, "Hot Beverages", "#FF0000", 1, false, Instant.now(), Instant.now()
        );

        when(manageCategoryUseCase.updateCategory(eq(cafeId), eq(categoryId), any(UpdateCategoryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.name").value("Hot Beverages"));

        verify(manageCategoryUseCase).updateCategory(eq(cafeId), eq(categoryId), any(UpdateCategoryRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/categories/{id}/archive should archive category")
    void shouldArchiveCategory() throws Exception {
        CategoryResponse response = new CategoryResponse(
                categoryId, cafeId, "Beverages", "#FF0000", 1, true, Instant.now(), Instant.now()
        );

        when(manageCategoryUseCase.archiveCategory(cafeId, categoryId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/categories/{id}/archive", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.archived").value(true));

        verify(manageCategoryUseCase).archiveCategory(cafeId, categoryId);
    }
}
