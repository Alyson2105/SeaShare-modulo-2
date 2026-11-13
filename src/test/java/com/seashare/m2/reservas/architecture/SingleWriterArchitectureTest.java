package com.seashare.m2.reservas.architecture;

import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.seashare.m2", importOptions = ImportOption.DoNotIncludeTests.class) // solo produccion
class SingleWriterArchitectureTest { // T-24: el estado de una reserva solo lo escribe el motor CU-08

    @ArchTest
    static final ArchRule onlyStatusEngineCreatesReservations =
            noClasses().that().resideInAPackage("..reservas.application..")
                    .and().doNotHaveSimpleName("ChangeReservationStatusService") // unico escritor de estado
                    .should().callMethodWhere(new DescribedPredicate<>("crear reservas fuera de CU-08") {
                        @Override
                        public boolean test(JavaMethodCall call) { // evalua cada llamada del codigo analizado
                            return "create".equals(call.getTarget().getName()) // metodo de creacion
                                    && call.getTarget().getOwner().isAssignableTo(ReservationRepositoryPort.class); // del puerto de reservas
                        }
                    })
                    .because("la creacion debe pasar por CU-08 para auditar y encolar en la misma transaccion");
}
