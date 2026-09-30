package com.mylog.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.mylog")
class ModuleArchitectureTest {

    @ArchTest
    static final ArchRule domain_is_framework_independent = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "..api..",
                    "..infrastructure.."
            )
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule api_does_not_access_infrastructure = noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .allowEmptyShould(true);
}
