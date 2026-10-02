package br.com.fiap.phase4.billing.architecture;

import br.com.fiap.phase4.commons.test.architecture.CommonArchitectureRules;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "br.com.fiap.phase4.billing", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    public static final ArchTests commonRules = ArchTests.in(CommonArchitectureRules.class);

    @ArchTest
    public static final ArchRule domainMustNotDependOnJpa =
            noClasses().that().resideInAPackage("..billing.domain..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                    .because("Hexagonal domain model must remain decoupled from JPA persistence annotations");
}
