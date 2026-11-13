package com.seashare.m2.reservas.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.seashare.m2", importOptions = ImportOption.DoNotIncludeTests.class) // solo produccion
class LayeredArchitectureTest { // verifica que las dependencias respeten la arquitectura hexagonal

    private static final String DOMAIN = "..reservas.domain.."; // capa de dominio de reservas

    private static final String APPLICATION = "..reservas.application.."; // capa de aplicacion de reservas

    private static final String INFRASTRUCTURE = "..reservas.infrastructure.."; // capa de infraestructura de reservas

    private static final String DISPUTA_DOMAIN = "..disputa.domain.."; // capa de dominio de disputa

    private static final String DISPUTA_APPLICATION = "..disputa.application.."; // capa de aplicacion de disputa

    private static final String DISPUTA_INFRASTRUCTURE = "..disputa.infrastructure.."; // capa de infraestructura de disputa

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

    @ArchTest
    static final ArchRule disputeDomainDoesNotDependOnOuterLayers =
            noClasses().that().resideInAPackage(DISPUTA_DOMAIN)
                    .should().dependOnClassesThat().resideInAnyPackage(DISPUTA_APPLICATION, DISPUTA_INFRASTRUCTURE); // H9: dominio puro

    @ArchTest
    static final ArchRule disputeApplicationDoesNotDependOnInfrastructure =
            noClasses().that().resideInAPackage(DISPUTA_APPLICATION)
                    .should().dependOnClassesThat().resideInAPackage(DISPUTA_INFRASTRUCTURE); // H9: solo puertos y dominio

    @ArchTest
    static final ArchRule disputeDomainHasNoFrameworkDependencies =
            noClasses().that().resideInAPackage(DISPUTA_DOMAIN)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..", "jakarta.persistence..", "org.springframework.amqp.."); // H9: sin Spring, JPA ni AMQP

    @ArchTest
    static final ArchRule reservasDoesNotDependOnDisputa =
            noClasses().that().resideInAPackage("..reservas..")
                    .should().dependOnClassesThat().resideInAPackage("..disputa.."); // D-03: solo via shared/contracts

    @ArchTest
    static final ArchRule disputaDoesNotDependOnReservas =
            noClasses().that().resideInAPackage("..disputa..")
                    .should().dependOnClassesThat().resideInAPackage("..reservas.."); // D-03: solo via shared/contracts
}
