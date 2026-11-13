package com.seashare.m2.reservas.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.math.BigDecimal;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.seashare.m2") // analiza las clases de produccion del proyecto
class MoneyArchitectureTest { // verifica que M2 nunca haga aritmetica monetaria

    private static final Set<String> FORBIDDEN_METHODS = Set.of( // operaciones prohibidas sobre BigDecimal
            "add",
            "subtract",
            "multiply",
            "divide",
            "setScale",
            "negate",
            "remainder",
            "pow");

    @ArchTest
    static final ArchRule noBigDecimalArithmetic =
            noClasses().that().resideInAPackage("com.seashare.m2..")
                    .should().callMethodWhere(new DescribedPredicate<>("invocar aritmetica de BigDecimal") {
                        @Override
                        public boolean test(JavaMethodCall call) { // evalua cada llamada del codigo analizado
                            return call.getTarget().getOwner().isAssignableTo(BigDecimal.class) // el metodo pertenece a BigDecimal
                                    && FORBIDDEN_METHODS.contains(call.getTarget().getName()); // y es una operacion aritmetica prohibida
                        }
                    })
                    .because("M2 solo transporta importes; sumar, restar, multiplicar, dividir o redondear corresponde a M3");
}
