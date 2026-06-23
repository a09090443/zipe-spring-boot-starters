package com.zipe.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Starter 架構結構測試（harness「機械化強制」原則的落地）。
 *
 * <p>以 ArchUnit 守住自動配置的共通慣例，讓不符規範的程式碼在 {@code mvn verify}
 * 階段即被 CI 擋下，而非靠人工 review 或 agent 自律。
 *
 * <p>本檔在<b>每個 {@code *-spring-boot-starter} reactor 模組各放一份，內容相同</b>；
 * 規則對應 {@code .claude/skills/authoring-a-starter} 黃金規範，調整規範時兩處一併更新。
 * 掃描範圍為 classpath 上的 {@code com.zipe} 類別（reactor 建構時會含相依模組），故全專案皆須通過。
 */
@AnalyzeClasses(
        packages = "com.zipe",
        importOptions = ImportOption.DoNotIncludeTests.class)
class StarterArchitectureTest {

    /** 標註 {@code @AutoConfiguration} 的類別必須位於名為 {@code autoconfiguration} 的套件下。 */
    @ArchTest
    static final ArchRule 自動配置類別應置於_autoconfiguration_套件 =
            classes()
                    .that()
                    .areAnnotatedWith(AutoConfiguration.class)
                    .should()
                    .resideInAPackage("..autoconfiguration..")
                    .allowEmptyShould(true);

    /**
     * 位於 {@code autoconfiguration} 套件、且類名以 {@code AutoConfiguration} 結尾者，
     * 必須真正標註 {@code @AutoConfiguration}（避免徒有其名卻未註冊的「假」自動配置類）。
     */
    @ArchTest
    static final ArchRule 名為AutoConfiguration者必須有對應註解 =
            classes()
                    .that()
                    .resideInAPackage("..autoconfiguration..")
                    .and()
                    .haveSimpleNameEndingWith("AutoConfiguration")
                    .should()
                    .beAnnotatedWith(AutoConfiguration.class)
                    .allowEmptyShould(true);

    /** {@code @ConfigurationProperties} 設定屬性類別必須為 public，使用方才能正常綁定。 */
    @ArchTest
    static final ArchRule 設定屬性類別必須為public =
            classes()
                    .that()
                    .areAnnotatedWith(ConfigurationProperties.class)
                    .should()
                    .bePublic()
                    .allowEmptyShould(true);

    /** 正式程式碼（main）不得依賴測試套件，避免測試碼意外外洩到發佈產物。 */
    @ArchTest
    static final ArchRule 正式碼不得依賴測試套件 =
            noClasses()
                    .that()
                    .resideInAPackage("com.zipe..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..test..")
                    .allowEmptyShould(true);
}
