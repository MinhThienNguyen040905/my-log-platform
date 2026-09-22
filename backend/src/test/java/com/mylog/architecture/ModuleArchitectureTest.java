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
    void commonModuleDoesNotDependOnFeatureModules() {
        String[] featurePackages = FEATURE_MODULES.stream()
                .map(module -> "com.mylog." + module + "..")
                .toArray(String[]::new);

        noClasses()
                .that()
                .resideInAPackage("com.mylog.common..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(featurePackages)
                .because("common technical code must not depend on business feature modules")
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void servicesDoNotDependOnWebLayer() {
        noClasses()
                .that()
                .resideInAPackage("com.mylog..service..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "com.mylog..controller..",
                        "com.mylog..dto..")
                .because("HTTP DTO mapping belongs to controllers")
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void controllersDoNotDependOnRepositories() {
        noClasses()
                .that()
                .resideInAPackage("com.mylog..controller..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.mylog..repository..")
                .because("controllers must call services instead of repositories")
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void messagingAdaptersDoNotDependOnRepositories() {
        noClasses()
                .that()
                .resideInAPackage("com.mylog..messaging..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.mylog..repository..")
                .because("message consumers must call application services instead of repositories")
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void repositoriesDoNotDependOnUpperLayers() {
        noClasses()
                .that()
                .resideInAPackage("com.mylog..repository..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "com.mylog..controller..",
                        "com.mylog..dto..",
                        "com.mylog..service..")
                .because("repositories are persistence adapters and must not depend on orchestration or HTTP types")
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void servicesDoNotUsePersistenceFrameworksDirectly() {
        noClasses()
                .that()
                .resideInAPackage("com.mylog..service..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework.jdbc..",
                        "org.springframework.data.jpa..",
                        "org.springframework.data.redis..",
                        "jakarta.persistence..")
                .because("services orchestrate use cases through repositories or explicit ports")
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void entitiesDoNotDependOnUpperLayers() {
        noClasses()
                .that()
                .resideInAPackage("com.mylog..entity..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "com.mylog..controller..",
                        "com.mylog..dto..",
                        "com.mylog..service..",
                        "com.mylog..repository..",
                        "com.mylog..messaging..")
                .because("entities contain state and invariants, not orchestration or transport concerns")
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
