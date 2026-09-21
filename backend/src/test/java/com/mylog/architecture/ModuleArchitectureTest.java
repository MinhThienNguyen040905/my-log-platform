package com.mylog.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.mylog.MylogBackendApplication;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModuleArchitectureTest {

    private static final List<String> FEATURE_MODULES = List.of(
            "analysis",
            "feedback",
            "identity",
            "insight",
            "journal",
            "media",
            "reflection",
            "report",
            "safety",
            "statistics");

    private final JavaClasses productionClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackagesOf(MylogBackendApplication.class);

    @Test
    void featureModulesDoNotDependDirectlyOnEachOther() {
        assertThatNoException().isThrownBy(() -> {
            for (String sourceModule : FEATURE_MODULES) {
                String[] forbiddenTargets = FEATURE_MODULES.stream()
                        .filter(module -> !module.equals(sourceModule))
                        .map(module -> "com.mylog." + module + "..")
                        .toArray(String[]::new);

                noClasses()
                        .that()
                        .resideInAPackage("com.mylog." + sourceModule + "..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(forbiddenTargets)
                        .because("feature modules communicate through explicit application ports or events")
                        .allowEmptyShould(true)
                        .check(productionClasses);
            }
        });
    }

    @Test
    void sharedModuleDoesNotDependOnFeatureModules() {
        String[] featurePackages = FEATURE_MODULES.stream()
                .map(module -> "com.mylog." + module + "..")
                .toArray(String[]::new);

        noClasses()
                .that()
                .resideInAPackage("com.mylog.shared..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(featurePackages)
                .because("shared technical code must not depend on business feature modules")
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void journalCoreDoesNotDependOnRedisOrMessaging() {
        noClasses()
                .that()
                .resideInAPackage("com.mylog.journal..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework.data.redis..",
                        "org.springframework.amqp..")
                .because("journal persistence must remain available without Redis or RabbitMQ")
                .allowEmptyShould(true)
                .check(productionClasses);
    }
}
