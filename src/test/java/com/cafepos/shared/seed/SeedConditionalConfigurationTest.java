package com.cafepos.shared.seed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SeedConditionalConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    ApiSeedController.class,
                    DatabaseSeeder.class
            );

    @Test
    @DisplayName("Default context should not register DatabaseSeeder or ApiSeedController when app.seed.enabled is false or unset")
    void shouldNotRegisterBeansByDefault() {
        contextRunner
                .withPropertyValues("app.seed.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DatabaseSeeder.class);
                    assertThat(context).doesNotHaveBean(ApiSeedController.class);
                });
    }

    @Test
    @DisplayName("Seed-enabled context should register both DatabaseSeeder and ApiSeedController when app.seed.enabled is true")
    void shouldRegisterBeansWhenSeedEnabled() {
        contextRunner
                .withBean(com.cafepos.organization.domain.CafeRepository.class, () -> org.mockito.Mockito.mock(com.cafepos.organization.domain.CafeRepository.class))
                .withBean(com.cafepos.identity.domain.EmployeeRepository.class, () -> org.mockito.Mockito.mock(com.cafepos.identity.domain.EmployeeRepository.class))
                .withBean(com.cafepos.identity.domain.UserAccountRepository.class, () -> org.mockito.Mockito.mock(com.cafepos.identity.domain.UserAccountRepository.class))
                .withBean(com.cafepos.catalog.domain.ProductCategoryRepository.class, () -> org.mockito.Mockito.mock(com.cafepos.catalog.domain.ProductCategoryRepository.class))
                .withBean(com.cafepos.catalog.domain.ProductRepository.class, () -> org.mockito.Mockito.mock(com.cafepos.catalog.domain.ProductRepository.class))
                .withBean(com.cafepos.floor.domain.FloorRepository.class, () -> org.mockito.Mockito.mock(com.cafepos.floor.domain.FloorRepository.class))
                .withBean(com.cafepos.floor.domain.DiningTableRepository.class, () -> org.mockito.Mockito.mock(com.cafepos.floor.domain.DiningTableRepository.class))
                .withBean(com.cafepos.session.domain.TerminalRepository.class, () -> org.mockito.Mockito.mock(com.cafepos.session.domain.TerminalRepository.class))
                .withBean(com.cafepos.promotion.domain.CouponRepository.class, () -> org.mockito.Mockito.mock(com.cafepos.promotion.domain.CouponRepository.class))
                .withBean(com.cafepos.identity.infrastructure.PasswordEncodingService.class, () -> org.mockito.Mockito.mock(com.cafepos.identity.infrastructure.PasswordEncodingService.class))
                .withPropertyValues("app.seed.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(DatabaseSeeder.class);
                    assertThat(context).hasSingleBean(ApiSeedController.class);
                });
    }
}
