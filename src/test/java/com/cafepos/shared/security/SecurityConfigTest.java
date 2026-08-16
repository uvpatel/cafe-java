package com.cafepos.shared.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@SpringJUnitConfig
@WebAppConfiguration
@Import({SecurityConfig.class, SecurityConfigTest.TestEndpoints.class})
class SecurityConfigTest {

    @Autowired private WebApplicationContext applicationContext;

    @MockBean private TenantAuthenticationFilter tenantAuthenticationFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(
                        invocation -> {
                            FilterChain chain = invocation.getArgument(2);
                            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                            return null;
                        })
                .when(tenantAuthenticationFilter)
                .doFilter(any(), any(), any());

        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("Business APIs reject anonymous requests")
    void protectedEndpointRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/products")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authentication endpoints remain public")
    void authenticationEndpointRemainsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/auth/test")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated users can access business APIs")
    void protectedEndpointAllowsAuthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/products").with(user("employee")))
                .andExpect(status().isOk());
    }

    @RestController
    static class TestEndpoints {

        @GetMapping("/api/v1/auth/test")
        String publicEndpoint() {
            return "public";
        }

        @GetMapping("/api/v1/products")
        String protectedEndpoint() {
            return "protected";
        }
    }
}
