package com.cafepos.catalog.api;

import com.cafepos.catalog.application.ManageProductUseCase;
import com.cafepos.catalog.domain.UnitOfMeasure;
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
class ProductControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ManageProductUseCase manageProductUseCase;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ProductController controller = new ProductController(manageProductUseCase);
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
    @DisplayName("GET /api/v1/products should return list of products")
    void shouldGetProducts() throws Exception {
        ProductResponse response = new ProductResponse(
                productId, cafeId, categoryId, "Espresso", "ESP-001",
                new BigDecimal("3.5000"), "USD", UnitOfMeasure.UNIT, true, false,
                Instant.now(), Instant.now()
        );

        when(manageProductUseCase.getProductsByCafe(cafeId, null, false)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(productId.toString()))
                .andExpect(jsonPath("$[0].name").value("Espresso"))
                .andExpect(jsonPath("$[0].sku").value("ESP-001"))
                .andExpect(jsonPath("$[0].priceAmount").value(3.5000))
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[0].unitOfMeasure").value("UNIT"))
                .andExpect(jsonPath("$[0].kitchenRouted").value(true));

        verify(manageProductUseCase).getProductsByCafe(cafeId, null, false);
    }

    @Test
    @DisplayName("GET /api/v1/products with categoryId filter should pass parameter")
    void shouldGetProductsFilteredByCategory() throws Exception {
        ProductResponse response = new ProductResponse(
                productId, cafeId, categoryId, "Espresso", "ESP-001",
                new BigDecimal("3.5000"), "USD", UnitOfMeasure.UNIT, true, false,
                Instant.now(), Instant.now()
        );

        when(manageProductUseCase.getProductsByCafe(cafeId, categoryId, false)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/products?categoryId=" + categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(productId.toString()));

        verify(manageProductUseCase).getProductsByCafe(cafeId, categoryId, false);
    }

    @Test
    @DisplayName("POST /api/v1/products should create product and return 201 CREATED")
    void shouldCreateProduct() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Cappuccino", "CAP-001", new BigDecimal("4.50"), "USD", UnitOfMeasure.UNIT, true
        );
        ProductResponse response = new ProductResponse(
                productId, cafeId, categoryId, "Cappuccino", "CAP-001",
                new BigDecimal("4.5000"), "USD", UnitOfMeasure.UNIT, true, false,
                Instant.now(), Instant.now()
        );

        when(manageProductUseCase.createProduct(eq(cafeId), any(CreateProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.name").value("Cappuccino"))
                .andExpect(jsonPath("$.sku").value("CAP-001"))
                .andExpect(jsonPath("$.priceAmount").value(4.5000));

        verify(manageProductUseCase).createProduct(eq(cafeId), any(CreateProductRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/products with invalid request should return 400 Bad Request")
    void shouldReturn400ForInvalidCreateProduct() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                null, "", "SKU-TOO-LONG-EXPANDING-OVER-FIFTY-CHARACTERS-LIMIT-TEST", new BigDecimal("-1.00"), "INVALID", null, null
        );

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} should update product details")
    void shouldUpdateProduct() throws Exception {
        UpdateProductRequest request = new UpdateProductRequest(
                categoryId, "Latte", "LAT-001", new BigDecimal("4.00"), "USD", UnitOfMeasure.UNIT, true
        );
        ProductResponse response = new ProductResponse(
                productId, cafeId, categoryId, "Latte", "LAT-001",
                new BigDecimal("4.0000"), "USD", UnitOfMeasure.UNIT, true, false,
                Instant.now(), Instant.now()
        );

        when(manageProductUseCase.updateProduct(eq(cafeId), eq(productId), any(UpdateProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.name").value("Latte"));

        verify(manageProductUseCase).updateProduct(eq(cafeId), eq(productId), any(UpdateProductRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/products/{id}/archive should archive product")
    void shouldArchiveProduct() throws Exception {
        ProductResponse response = new ProductResponse(
                productId, cafeId, categoryId, "Espresso", "ESP-001",
                new BigDecimal("3.5000"), "USD", UnitOfMeasure.UNIT, true, true,
                Instant.now(), Instant.now()
        );

        when(manageProductUseCase.archiveProduct(cafeId, productId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/products/{id}/archive", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.archived").value(true));

        verify(manageProductUseCase).archiveProduct(cafeId, productId);
    }
}
