package com.cafepos.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;

@AnalyzeClasses(packages = "com.cafepos", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureBoundariesTest {

    @ArchTest
    static final ArchRule no_field_injection = noFields()
            .should().beAnnotatedWith(Autowired.class)
            .because("Constructor injection must be used exclusively across all modules and services");

    @ArchTest
    static final ArchRule domain_layer_should_not_depend_on_transport_or_infrastructure = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..api..", "..infrastructure..")
            .because("Domain models must remain pure and free from transport controllers or infrastructure implementation details");
}
