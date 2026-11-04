package com.spacedrep.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Regras arquiteturais executaveis: o desenho da SPEC secao 6 vira teste.
 * Sem isso, "arquitetura em camadas" e so intencao no README.
 */
@AnalyzeClasses(packages = "com.spacedrep", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_is_framework_free =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "jakarta.servlet..");

    @ArchTest
    static final ArchRule application_does_not_know_infrastructure =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule api_does_not_know_persistence =
            noClasses().that().resideInAPackage("..api..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..infrastructure.persistence..", "..infrastructure.llm..");

    @ArchTest
    static final ArchRule controllers_are_only_in_api =
            noClasses().that().haveSimpleNameEndingWith("Controller")
                    .should().resideOutsideOfPackage("..api.controller");
}
