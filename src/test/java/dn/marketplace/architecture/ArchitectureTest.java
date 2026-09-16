package dn.marketplace.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Исполняемая версия правил изоляции из {@code .agents/CLAUDE.md}.
 * <p>
 * Эти тесты введены в Фазе A намеренно рано. Причина: документация проекта уже один раз
 * разошлась с кодом — {@code MEMORY.md} описывал схему, которой в репозитории не было.
 * Правило, которое проверяет только человек на ревью, разъезжается с кодом;
 * правило, которое валит сборку, — нет.
 * <p>
 * Правила, нарушаемые текущим кодом, обёрнуты в {@link FreezingArchRule}: существующие
 * нарушения зафиксированы в сторе и не валят сборку, новые — валят. Стор уменьшается
 * по мере выполнения задач B1-B6 и служит списком техдолга.
 */
@AnalyzeClasses(
        packages = "dn.marketplace",
        importOptions = com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // ------------------------------------------------------------------
    // Изоляция домена: entity и repository — внутренности, а не контракт
    // ------------------------------------------------------------------

    /** Нарушается сейчас: AccountEntity лежит в dn.marketplace.account.api. */
    @ArchTest
    static final ArchRule entities_live_in_the_entity_package = FreezingArchRule.freeze(
            classes().that().areAnnotatedWith(Entity.class)
                    .should().resideInAPackage("dn.marketplace.*.entity..")
                    .as("JPA-сущности должны лежать в dn.marketplace.<домен>.entity, а не в публичном api"));

    /** Нарушается сейчас: OrderEntity и ProductEntity всё ещё в api (фазы D/C). */
    @ArchTest
    static final ArchRule repositories_live_in_the_repository_package = FreezingArchRule.freeze(
            classes().that().areAssignableTo(Repository.class)
                    .should().resideInAPackage("dn.marketplace.*.repository..")
                    .as("Spring Data интерфейсы должны лежать в dn.marketplace.<домен>.repository"));

    // Package-private на entity/repository снят (решение №9): подпакеты и package-private
    // в Java несовместимы. Изоляцию держит domainInternalsAreHidden.

    // ------------------------------------------------------------------
    // Контракт наружу: DTO не тащат за собой модель БД
    // ------------------------------------------------------------------

    /**
     * DTO не должны ссылаться на JPA-сущности. Стор уменьшается по мере доменов.
     */
    @ArchTest
    static final ArchRule dto_do_not_expose_entities = FreezingArchRule.freeze(
            noClasses().that().resideInAPackage("..api.dto..")
                    .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
                    .as("DTO не должны ссылаться на JPA-сущности"));

    // ------------------------------------------------------------------
    // Междоменное общение: только через *Facade
    // ------------------------------------------------------------------

    @ArchTest
    static final ArchRule account_internals_are_hidden = domainInternalsAreHidden("account");

    @ArchTest
    static final ArchRule order_internals_are_hidden = domainInternalsAreHidden("order");

    @ArchTest
    static final ArchRule product_internals_are_hidden = domainInternalsAreHidden("product");

    private static ArchRule domainInternalsAreHidden(String domain) {
        String base = "dn.marketplace." + domain;
        return noClasses()
                .that().resideOutsideOfPackage(base + "..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(base + ".entity..", base + ".repository..", base + ".service..")
                .as("вне домена " + domain + " нельзя зависеть от его entity, repository и service — только от " + domain + ".*Facade")
                .allowEmptyShould(true);
    }

    // ------------------------------------------------------------------
    // Время: только Instant
    // ------------------------------------------------------------------

    /**
     * Правило MEMORY.md: системные даты — исключительно java.time.Instant.
     * LocalDateTime и OffsetDateTime создают иллюзию локальной таймзоны и приводят
     * к сдвигам при переносе между окружениями.
     */
    @ArchTest
    static final ArchRule only_instant_is_used_for_time =
            noClasses().should().dependOnClassesThat()
                    .haveFullyQualifiedName("java.time.LocalDateTime")
                    .orShould().dependOnClassesThat()
                    .haveFullyQualifiedName("java.time.OffsetDateTime")
                    .orShould().dependOnClassesThat()
                    .haveFullyQualifiedName("java.util.Date")
                    .orShould().dependOnClassesThat()
                    .haveFullyQualifiedName("java.sql.Timestamp")
                    .as("для системных дат используем только java.time.Instant")
                    .allowEmptyShould(true);

    // ------------------------------------------------------------------
    // Размещение слоёв
    // ------------------------------------------------------------------

    @ArchTest
    static final ArchRule controllers_live_in_the_controller_package =
            classes().that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .should().resideInAPackage("..api.controller..")
                    .as("контроллеры должны лежать в <домен>.api.controller")
                    .allowEmptyShould(true);

    /**
     * *Facade — публичная дверь домена, поэтому лежит в корне пакета домена,
     * а не внутри api, service или entity.
     */
    @ArchTest
    static final ArchRule facades_live_in_the_domain_root =
            classes().that().haveSimpleNameEndingWith("Facade")
                    .should().resideInAPackage("dn.marketplace.*")
                    .as("*Facade должен лежать в корне пакета домена")
                    .allowEmptyShould(true);
}
