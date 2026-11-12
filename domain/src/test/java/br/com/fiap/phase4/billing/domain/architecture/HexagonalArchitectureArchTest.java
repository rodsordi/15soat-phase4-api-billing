package br.com.fiap.phase4.billing.domain.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "br.com.fiap.phase4.billing.domain",
        importOptions = {ImportOption.DoNotIncludeTests.class}
)
public class HexagonalArchitectureArchTest {

    @ArchTest
    public static final ArchRule domainMustNotDependOnWebOrServlets =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..org.springframework.web..", "..jakarta.servlet..", "..javax.servlet..");

    @ArchTest
    public static final ArchRule inputPortsMustBeInterfaces =
            classes()
                    .that().resideInAPackage("..domain.port.in..")
                    .and().areTopLevelClasses()
                    .should().beInterfaces();

    @ArchTest
    public static final ArchRule outputPortsMustBeInterfaces =
            classes()
                    .that().resideInAPackage("..domain.port.out..")
                    .and().areTopLevelClasses()
                    .should().beInterfaces();

    @ArchTest
    public static final ArchRule useCasesMustResideInUseCasePackage =
            classes()
                    .that().haveSimpleNameEndingWith("UseCaseImpl")
                    .should().resideInAPackage("..domain.usecase..");
}
