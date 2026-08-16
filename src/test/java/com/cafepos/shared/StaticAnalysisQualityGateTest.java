package com.cafepos.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StaticAnalysisQualityGateTest {

    @Test
    @DisplayName("Verify PMD and SpotBugs ruleset configuration files exist")
    void verifyStaticAnalysisConfigurationsExist() {
        File pmdRuleset = new File("config/pmd/pmd-ruleset.xml");
        assertThat(pmdRuleset).exists().isFile();

        File spotbugsFilter = new File("config/spotbugs/spotbugs-security-include.xml");
        assertThat(spotbugsFilter).exists().isFile();
    }
}
