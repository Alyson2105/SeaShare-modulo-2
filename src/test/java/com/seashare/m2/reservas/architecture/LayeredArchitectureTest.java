package com.seashare.m2.reservas.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.seashare.m2") // analiza las clases de produccion del proyecto
class LayeredArchitectureTest { // verifica que las dependencias respeten la arquitectura hexagonal

    private static final String DOMAIN = "..reservas.domain.."; // capa de dominio
    private static final String APPLICATION = "..reservas.application.."; // capa de aplicacion
    private static final String INFRASTRUCTURE = "..reservas.infrastructure.."; // capa de infraestructura

    @ArchTest
    static final ArchRule domainDoesNotDependOnOuterLayers =
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, INFRASTRUCTURE); // el dominio no conoce capas externas

    @ArchTest
    static final ArchRule applicationDoesNotDependOnInfrastructure =
            noClasses().that().resideInAPackage(APPLICATION)
                    .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE); // la aplicacion solo depende de puertos y dominio

    @ArchTest
    static final ArchRule inboundAdaptersDoNotDependOnOutboundAdapters =
            noClasses().that().resideInAPackage("..infrastructure.adapter.in..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure.adapter.out.."); // los adaptadores de entrada no conocen los de salida
}
