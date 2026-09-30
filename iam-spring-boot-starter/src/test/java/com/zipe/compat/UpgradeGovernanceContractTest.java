package com.zipe.compat;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Spring Boot 4.0.0 → 4.0.8 修補升級的治理契約測試（repo 層級，放在最深相依鏈 iam）。
 *
 * <p>守住升級只動 Boot 修補版本本身：</p>
 * <ul>
 *   <li>版本：根 parent 精確為 4.0.8、{@code java.version}=17、{@code project.version}=4.0.0.1，
 *       7 個子 pom 皆繼承根 pom、不覆寫 Boot 版本、不匯入其他 BOM；</li>
 *   <li>相依：第三方版本 properties 與各 starter 相依清單等於升級前（commit {@code 9972e45}）固定基準，
 *       新增者僅限 iam 兩個未寫版本的 test scope 相依；logon 不引用 iam；</li>
 *   <li>文件：升級說明 §9 內容、§1–§8 未被改寫、現況文件寫 4.0.8、範例文件照實寫 4.0.0；</li>
 *   <li>品質：7 個 starter 沒有停用、跳過或排除測試。</li>
 * </ul>
 * 期望值皆寫死於本檔，不在執行期由待驗的 pom 或文件重建。
 */
class UpgradeGovernanceContractTest {

    private static final String BOOT_VERSION = "4.0.8";

    private static final String STARTER_VERSION = "4.0.0.1";

    private static final List<String> MODULES = List.of(
            "base-spring-boot-starter",
            "db-spring-boot-starter",
            "job-spring-boot-starter",
            "logon-spring-boot-starter",
            "iam-spring-boot-starter",
            "web-spring-boot-starter",
            "web-service-spring-boot-starter");

    /** 升級前根 pom 的全部 properties（含第三方與外掛版本）；升級不得增刪改。 */
    private static final Map<String, String> BASELINE_ROOT_PROPERTIES = orderedMap(
            "java.version", "17",
            "project.build.sourceEncoding", "UTF-8",
            "project.reporting.outputEncoding", "UTF-8",
            "commons-collections4.version", "4.5.0",
            "commons-io.version", "2.22.0",
            "commons-beanutils.version", "1.11.0",
            "velocity-engine-core.version", "2.4.1",
            "poi.version", "5.2.5",
            "jasperreports.version", "7.0.7",
            "okhttp3.version", "4.12.0",
            "p6spy.version", "3.9.1",
            "jt400.version", "20.0.6",
            "cxf-spring-boot-starter-jaxws.version", "4.2.2",
            "cxf-rt-databinding-jaxb.version", "4.2.2",
            "jaxws-ri.version", "4.0.1",
            "jjwt.version", "0.12.6",
            "maven-source-plugin.version", "3.4.0",
            "maven-javadoc-plugin.version", "3.6.3",
            "maven-gpg-plugin.version", "3.2.8",
            "central-publishing-maven-plugin.version", "0.11.0",
            "jacoco-maven-plugin.version", "0.8.15",
            "spotless-maven-plugin.version", "3.7.0",
            "maven-dependency-plugin.version", "3.8.1",
            "archunit-junit5.version", "1.3.0",
            "greenmail-junit5.version", "2.1.7",
            "gpg.keyname", "063A3F99C99FC14AB63FA41F451A84C7C09C90F0");

    /**
     * 升級前（9972e45）各 starter 宣告的非 test 相依，格式 {@code groupId:artifactId:scope[:optional]}，
     * 未寫 scope 視為 compile。
     */
    private static final Map<String, Set<String>> BASELINE_MAIN_DEPENDENCIES = Map.of(
            "base-spring-boot-starter", Set.of(
                    "org.springframework.boot:spring-boot-starter:compile",
                    "org.springframework.boot:spring-boot-autoconfigure:compile",
                    "org.springframework.boot:spring-boot-configuration-processor:compile:optional",
                    "org.springframework.boot:spring-boot-starter-json:compile",
                    "org.springframework.boot:spring-boot-starter-mail:compile",
                    "org.projectlombok:lombok:compile:optional",
                    "org.apache.commons:commons-lang3:compile",
                    "org.apache.commons:commons-collections4:compile",
                    "commons-io:commons-io:compile",
                    "commons-beanutils:commons-beanutils:compile",
                    "ch.qos.logback:logback-core:compile",
                    "ch.qos.logback:logback-classic:compile",
                    "org.aspectj:aspectjweaver:compile",
                    "com.google.code.gson:gson:compile",
                    "org.apache.velocity:velocity-engine-core:compile",
                    "jakarta.servlet:jakarta.servlet-api:compile",
                    "org.apache.poi:poi:compile",
                    "org.apache.poi:poi-ooxml:compile",
                    "net.sf.jasperreports:jasperreports:compile",
                    "net.sf.jasperreports:jasperreports-pdf:compile",
                    "com.squareup.okhttp3:okhttp:compile"),
            "db-spring-boot-starter", Set.of(
                    "org.springframework.boot:spring-boot-autoconfigure:compile",
                    "org.springframework.boot:spring-boot-configuration-processor:compile:optional",
                    "org.springframework.boot:spring-boot-starter-data-jdbc:compile",
                    "org.springframework.boot:spring-boot-starter-data-jpa:compile",
                    "org.springframework.boot:spring-boot-starter-jdbc:compile",
                    "org.projectlombok:lombok:compile:optional",
                    "io.github.a09090443:base-spring-boot-starter:compile",
                    "p6spy:p6spy:compile",
                    "com.microsoft.sqlserver:mssql-jdbc:compile",
                    "com.mysql:mysql-connector-j:compile",
                    "org.mariadb.jdbc:mariadb-java-client:compile",
                    "net.sf.jt400:jt400:compile",
                    "org.postgresql:postgresql:compile"),
            "job-spring-boot-starter", Set.of(
                    "org.springframework.boot:spring-boot-starter-web:compile",
                    "org.springframework.boot:spring-boot-starter-quartz:compile",
                    "org.springframework.boot:spring-boot-starter-jdbc:compile",
                    "org.projectlombok:lombok:compile:optional",
                    "com.h2database:h2:provided",
                    "io.github.a09090443:base-spring-boot-starter:compile"),
            "logon-spring-boot-starter", Set.of(
                    "org.springframework.boot:spring-boot-autoconfigure:compile",
                    "org.springframework.boot:spring-boot-configuration-processor:compile:optional",
                    "org.springframework.boot:spring-boot-starter-security:compile",
                    "org.springframework.boot:spring-boot-starter-web:compile",
                    "org.projectlombok:lombok:compile:optional",
                    "io.github.a09090443:base-spring-boot-starter:compile",
                    "io.jsonwebtoken:jjwt-api:compile",
                    "io.jsonwebtoken:jjwt-impl:compile",
                    "io.jsonwebtoken:jjwt-jackson:compile"),
            "iam-spring-boot-starter", Set.of(
                    "org.springframework.boot:spring-boot-autoconfigure:compile",
                    "org.springframework.boot:spring-boot-configuration-processor:compile:optional",
                    "org.springframework.boot:spring-boot-starter-data-jpa:compile",
                    "io.github.a09090443:logon-spring-boot-starter:compile",
                    "org.projectlombok:lombok:compile:optional"),
            "web-spring-boot-starter", Set.of(
                    "org.springframework.boot:spring-boot-starter-thymeleaf:compile",
                    "org.springframework.boot:spring-boot-starter-web:compile",
                    "org.springframework.boot:spring-boot-autoconfigure:compile",
                    "org.springframework.boot:spring-boot-configuration-processor:compile:optional",
                    "org.projectlombok:lombok:compile:optional",
                    "org.apache.tomcat.embed:tomcat-embed-jasper:provided",
                    "jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api:compile",
                    "org.glassfish.web:jakarta.servlet.jsp.jstl:compile",
                    "io.github.a09090443:base-spring-boot-starter:compile"),
            "web-service-spring-boot-starter", Set.of(
                    "org.springframework.boot:spring-boot-autoconfigure:compile",
                    "org.springframework.boot:spring-boot-configuration-processor:compile:optional",
                    "org.projectlombok:lombok:compile:optional",
                    "org.apache.cxf:cxf-spring-boot-starter-jaxws:compile",
                    "org.apache.cxf:cxf-rt-databinding-jaxb:compile",
                    "com.sun.xml.ws:jaxws-ri:compile",
                    "tools.jackson.dataformat:jackson-dataformat-xml:compile",
                    "io.github.a09090443:base-spring-boot-starter:compile",
                    "org.apache.httpcomponents.client5:httpclient5:compile"));

    /** 升級前（9972e45）各 starter 的 test scope 相依（{@code groupId:artifactId}）。 */
    private static final Map<String, Set<String>> BASELINE_TEST_DEPENDENCIES = Map.of(
            "base-spring-boot-starter", Set.of(
                    "org.springframework.boot:spring-boot-starter-test",
                    "com.tngtech.archunit:archunit-junit5",
                    "com.icegreen:greenmail-junit5"),
            "db-spring-boot-starter", Set.of(
                    "org.junit.jupiter:junit-jupiter",
                    "com.h2database:h2",
                    "com.tngtech.archunit:archunit-junit5"),
            "job-spring-boot-starter", Set.of(
                    "org.junit.jupiter:junit-jupiter",
                    "com.tngtech.archunit:archunit-junit5"),
            "logon-spring-boot-starter", Set.of(
                    "org.springframework.security:spring-security-test",
                    "org.springframework.boot:spring-boot-starter-test",
                    "com.tngtech.archunit:archunit-junit5",
                    "org.springframework.boot:spring-boot-webmvc-test",
                    "org.junit.jupiter:junit-jupiter",
                    "org.mockito:mockito-core"),
            "iam-spring-boot-starter", Set.of(
                    "org.springframework.boot:spring-boot-starter-test",
                    "com.tngtech.archunit:archunit-junit5",
                    "com.h2database:h2"),
            "web-spring-boot-starter", Set.of(
                    "org.springframework.boot:spring-boot-starter-test",
                    "com.tngtech.archunit:archunit-junit5"),
            "web-service-spring-boot-starter", Set.of(
                    "org.junit.jupiter:junit-jupiter",
                    "com.tngtech.archunit:archunit-junit5"));

    /** 本次升級核准新增的 test scope 相依（僅 iam）。 */
    private static final Map<String, Set<String>> APPROVED_NEW_TEST_DEPENDENCIES = Map.of(
            "iam-spring-boot-starter", Set.of(
                    "org.springframework.security:spring-security-test",
                    "org.springframework.boot:spring-boot-webmvc-test"));

    /** 升級前 UPGRADE-SPRING-BOOT-4.md 的 git blob SHA-1（{@code git rev-parse 9972e45:UPGRADE-SPRING-BOOT-4.md}）。 */
    private static final String BASELINE_UPGRADE_DOC_BLOB = "6c24e0055dacabe29ca3ea34ba8523517bfde091";

    /** §9 標題前的分隔（升級前檔案結尾無換行，§9 以此接續）。 */
    private static final String SECTION_9_SEPARATOR = "\n\n---\n\n## 9. Spring Boot 4.0.0 → 4.0.8 修補升級";

    /** 把 Boot 3.5.x 或 4.0.0 寫成現況版本的敘述。 */
    private static final Pattern STALE_BOOT_VERSION =
            Pattern.compile("Spring Boot[ `*|:]{0,6}(3\\.5\\.[x\\d]+|4\\.0\\.0(?![.\\d]))");

    // ---------- 版本 ----------

    @Test
    void rootPomPinsExactBootJavaAndStarterVersions() throws Exception {
        Element project = parsePom(repoRoot().resolve("pom.xml"));
        Element parent = child(project, "parent");

        assertThat(childText(parent, "groupId")).isEqualTo("org.springframework.boot");
        assertThat(childText(parent, "artifactId")).isEqualTo("spring-boot-starter-parent");
        assertThat(childText(parent, "version")).isEqualTo(BOOT_VERSION);
        assertThat(childText(project, "version")).isEqualTo(STARTER_VERSION);
        assertThat(properties(project)).containsEntry("java.version", "17")
                .doesNotContainKey("spring-boot.version");
        assertThat(childTexts(child(project, "modules"), "module")).containsExactlyElementsOf(MODULES);
        assertThat(importedBoms(project)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "base-spring-boot-starter", "db-spring-boot-starter", "job-spring-boot-starter",
            "logon-spring-boot-starter", "iam-spring-boot-starter", "web-spring-boot-starter",
            "web-service-spring-boot-starter"})
    void childPomInheritsRootWithoutBootOverride(String module) throws Exception {
        Element project = parsePom(repoRoot().resolve(module).resolve("pom.xml"));
        Element parent = child(project, "parent");

        assertThat(childText(parent, "groupId")).isEqualTo("io.github.a09090443");
        assertThat(childText(parent, "artifactId")).isEqualTo("zipe-spring-boot-starters");
        assertThat(childText(parent, "version")).isEqualTo(STARTER_VERSION);
        assertThat(childText(parent, "relativePath")).isEqualTo("../pom.xml");
        assertThat(child(project, "version")).as("子模組需繼承根 pom 的 project.version").isNull();
        assertThat(properties(project)).doesNotContainKey("spring-boot.version");
        assertThat(importedBoms(project)).isEmpty();
        assertThat(child(project, "dependencyManagement")).isNull();
        for (Element dependency : dependencies(project)) {
            assertThat(child(dependency, "version"))
                    .as("%s 的 %s 不可自行指定版本", module, childText(dependency, "artifactId"))
                    .isNull();
        }
    }

    // ---------- 相依治理 ----------

    @Test
    void rootPropertiesMatchPreUpgradeBaseline() throws Exception {
        Map<String, String> actual = properties(parsePom(repoRoot().resolve("pom.xml")));
        assertThat(new TreeMap<>(actual)).isEqualTo(new TreeMap<>(BASELINE_ROOT_PROPERTIES));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "base-spring-boot-starter", "db-spring-boot-starter", "job-spring-boot-starter",
            "logon-spring-boot-starter", "iam-spring-boot-starter", "web-spring-boot-starter",
            "web-service-spring-boot-starter"})
    void dependenciesMatchPreUpgradeBaseline(String module) throws Exception {
        Set<String> main = new TreeSet<>();
        Set<String> test = new TreeSet<>();
        for (Element dependency : dependencies(parsePom(repoRoot().resolve(module).resolve("pom.xml")))) {
            String coordinate = childText(dependency, "groupId") + ":" + childText(dependency, "artifactId");
            String scope = child(dependency, "scope") == null ? "compile" : childText(dependency, "scope");
            if ("test".equals(scope)) {
                test.add(coordinate);
            } else {
                boolean optional = child(dependency, "optional") != null
                        && "true".equals(childText(dependency, "optional"));
                main.add(coordinate + ":" + scope + (optional ? ":optional" : ""));
            }
        }

        assertThat(main).as("%s 的 compile/runtime/provided 相依", module)
                .containsExactlyInAnyOrderElementsOf(BASELINE_MAIN_DEPENDENCIES.get(module));

        Set<String> expectedTest = new TreeSet<>(BASELINE_TEST_DEPENDENCIES.get(module));
        expectedTest.addAll(APPROVED_NEW_TEST_DEPENDENCIES.getOrDefault(module, Set.of()));
        assertThat(test).as("%s 的 test 相依", module).containsExactlyInAnyOrderElementsOf(expectedTest);
    }

    @Test
    void logonDoesNotDependOnOrReferenceIam() throws Exception {
        Path root = repoRoot();
        String logonPom = Files.readString(root.resolve("logon-spring-boot-starter/pom.xml"));
        assertThat(logonPom).doesNotContain("iam-spring-boot-starter");

        Path iamMain = root.resolve("iam-spring-boot-starter/src/main/java");
        Map<String, String> iamClasses = new TreeMap<>();
        for (Path source : javaFiles(iamMain)) {
            String relative = iamMain.relativize(source).toString().replace('\\', '/');
            String fqn = relative.substring(0, relative.length() - ".java".length()).replace('/', '.');
            iamClasses.put(fqn, fqn.substring(fqn.lastIndexOf('.') + 1));
        }
        assertThat(iamClasses).isNotEmpty();

        Path logonMain = root.resolve("logon-spring-boot-starter/src/main");
        List<String> violations = new ArrayList<>();
        for (Path source : allFiles(logonMain)) {
            String content = Files.readString(source);
            String packageName = packageOf(content);
            for (Map.Entry<String, String> iamClass : iamClasses.entrySet()) {
                String fqn = iamClass.getKey();
                boolean samePackage = fqn.substring(0, fqn.lastIndexOf('.')).equals(packageName);
                boolean referencesFqn = Pattern.compile("\\b" + Pattern.quote(fqn) + "\\b").matcher(content).find();
                boolean referencesSimpleName = samePackage
                        && Pattern.compile("\\b" + Pattern.quote(iamClass.getValue()) + "\\b").matcher(content).find();
                if (referencesFqn || referencesSimpleName) {
                    violations.add(logonMain.relativize(source) + " → " + fqn);
                }
            }
        }
        assertThat(violations).as("logon 主程式不可引用 iam 類別").isEmpty();
    }

    // ---------- 文件 ----------

    @Test
    void upgradeGuideKeepsHistoryAndRecordsPatchUpgrade() throws Exception {
        String doc = Files.readString(repoRoot().resolve("UPGRADE-SPRING-BOOT-4.md"));
        int separator = doc.indexOf(SECTION_9_SEPARATOR);
        assertThat(separator).as("§9 標題必須緊接在升級前內容之後").isPositive();

        String history = doc.substring(0, separator);
        assertThat(gitBlobSha1(history)).as("§1–§8 須與升級前原文完全相同").isEqualTo(BASELINE_UPGRADE_DOC_BLOB);

        String section9 = doc.substring(separator);
        assertThat(section9)
                .contains("| 根 `pom.xml` 的 `spring-boot-starter-parent` | 4.0.0 | **4.0.8** |")
                .contains("| Starter 發布版本（`project.version`） | 4.0.0.1 | 4.0.0.1（不變） |")
                .contains("### 9.2 必要調整")
                .contains("**程式／設定調整：無。**")
                .contains("**第三方相依版本變動：無。**")
                .contains("`starters_example` 與 `example-kotlin` **維持 Spring Boot 4.0.0**")
                .contains("範例升版留待後續任務跟進");
    }

    @Test
    void currentStateDocsDescribeBoot408() throws Exception {
        Path root = repoRoot();
        assertThat(Files.readString(root.resolve("AGENTS.md"))).contains("Java 17+、Spring Boot 4.0.8。");
        assertThat(Files.readString(root.resolve("README.md")))
                .contains("| **Spring Boot** | 4.0.x（Starter 以 4.0.8 建置與測試） |");
        assertThat(Files.readString(root.resolve("doc-site/docs/intro.md")))
                .contains("| **Spring Boot** | 4.0.x（Starter 以 4.0.8 建置與測試） |");
        assertThat(Files.readString(root.resolve("doc-site/docs/iam-starter/architecture.md")))
                .contains("本專案執行於 Spring Boot 4.0.8");
        assertThat(Files.readString(root.resolve("doc-site/docs/web-starter/architecture.md")))
                .contains("（Spring Boot 4.0.8，Java 17）");

        Set<Path> exampleDocs = Set.of(
                root.resolve("doc-site/docs/integration/index.md"),
                root.resolve("doc-site/docs/example-kotlin/index.md"));
        List<Path> currentStateDocs = new ArrayList<>();
        currentStateDocs.add(root.resolve("AGENTS.md"));
        currentStateDocs.add(root.resolve("README.md"));
        for (String module : MODULES) {
            Path readme = root.resolve(module).resolve("README.md");
            if (Files.isRegularFile(readme)) {
                currentStateDocs.add(readme);
            }
        }
        allFiles(root.resolve("doc-site/docs")).stream()
                .filter(path -> path.toString().endsWith(".md") || path.toString().endsWith(".mdx"))
                .filter(path -> !exampleDocs.contains(path))
                .forEach(currentStateDocs::add);

        List<String> stale = new ArrayList<>();
        for (Path doc : currentStateDocs) {
            Matcher matcher = STALE_BOOT_VERSION.matcher(Files.readString(doc));
            while (matcher.find()) {
                stale.add(root.relativize(doc) + ": " + matcher.group());
            }
        }
        assertThat(stale).as("現況文件不可把 3.5.x 或 4.0.0 寫成 Starter 的 Spring Boot 版本").isEmpty();
    }

    @Test
    void exampleDocsTruthfullyStateBoot400MatchingExampleBuilds() throws Exception {
        Path root = repoRoot();
        Element examplePom = parsePom(root.resolve("starters_example/pom.xml"));
        assertThat(childText(child(examplePom, "parent"), "version")).isEqualTo("4.0.0");
        assertThat(Files.readString(root.resolve("example-kotlin/build.gradle.kts")))
                .contains("id(\"org.springframework.boot\") version \"4.0.0\"");

        String integration = Files.readString(root.resolve("doc-site/docs/integration/index.md"));
        assertThat(integration)
                .contains("目前範例仍使用 Spring Boot `4.0.0`")
                .contains("範例升版留待後續任務跟進")
                .doesNotContain("已同步至目前版本");

        for (String path : List.of("doc-site/docs/example-kotlin/index.md", "example-kotlin/README.md")) {
            assertThat(Files.readString(root.resolve(path))).as(path)
                    .contains("Spring Boot 4.0.0 / Java 17")
                    .doesNotContain("4.0.8");
        }
    }

    // ---------- 測試未停用／未排除 ----------

    @Test
    void noStarterTestIsDisabledSkippedOrExcluded() throws Exception {
        Path root = repoRoot();
        List<String> disableMarkers = List.of(
                "@" + "Disabled", "@" + "EnabledIf", "@" + "DisabledIf", "@" + "EnabledOn", "Assumptions" + ".");
        List<String> violations = new ArrayList<>();
        for (String module : MODULES) {
            for (Path source : javaFiles(root.resolve(module).resolve("src/test/java"))) {
                if (source.getFileName().toString().equals("UpgradeGovernanceContractTest.java")) {
                    continue;
                }
                String content = Files.readString(source);
                disableMarkers.stream().filter(content::contains)
                        .forEach(marker -> violations.add(root.relativize(source) + " → " + marker));
            }
        }

        List<Path> poms = new ArrayList<>();
        poms.add(root.resolve("pom.xml"));
        MODULES.forEach(module -> poms.add(root.resolve(module).resolve("pom.xml")));
        List<String> pomMarkers = List.of(
                "skipTests", "maven.test.skip", "<skip>", "<excludes>", "<exclude>",
                "testFailureIgnore", "<test>", "<groups>", "excludedGroups");
        for (Path pom : poms) {
            String content = Files.readString(pom);
            pomMarkers.stream().filter(content::contains)
                    .forEach(marker -> violations.add(root.relativize(pom) + " → " + marker));
        }

        assertThat(violations).as("不可停用、跳過或排除測試").isEmpty();
    }

    // ---------- 工具 ----------

    private static Map<String, String> orderedMap(String... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    /** 與 {@code git hash-object} 相同的 blob SHA-1。 */
    private static String gitBlobSha1(String content) throws Exception {
        byte[] body = content.getBytes(StandardCharsets.UTF_8);
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(("blob " + body.length + "\0").getBytes(StandardCharsets.UTF_8));
        digest.update(body);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String packageOf(String source) {
        Matcher matcher = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;").matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static List<Path> javaFiles(Path dir) throws IOException {
        return allFiles(dir).stream().filter(path -> path.toString().endsWith(".java")).toList();
    }

    private static List<Path> allFiles(Path dir) throws IOException {
        assertThat(dir).isDirectory();
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
        }
    }

    private static Element parsePom(Path pom) throws Exception {
        assertThat(pom).isRegularFile();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
    }

    /** 直接子元素（不遞迴），不存在時回傳 null。 */
    private static Element child(Element parent, String tag) {
        if (parent == null) {
            return null;
        }
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && tag.equals(element.getTagName())) {
                return element;
            }
        }
        return null;
    }

    private static List<Element> children(Element parent, String tag) {
        List<Element> result = new ArrayList<>();
        if (parent == null) {
            return result;
        }
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element element && tag.equals(element.getTagName())) {
                result.add(element);
            }
        }
        return result;
    }

    private static String childText(Element parent, String tag) {
        Element element = child(parent, tag);
        assertThat(element).as("<%s> 必須存在", tag).isNotNull();
        return element.getTextContent().trim();
    }

    private static List<String> childTexts(Element parent, String tag) {
        return children(parent, tag).stream().map(element -> element.getTextContent().trim()).toList();
    }

    private static Map<String, String> properties(Element project) {
        Map<String, String> result = new LinkedHashMap<>();
        Element properties = child(project, "properties");
        if (properties == null) {
            return result;
        }
        NodeList nodes = properties.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element property) {
                result.put(property.getTagName(), property.getTextContent().trim());
            }
        }
        return result;
    }

    private static List<Element> dependencies(Element project) {
        return children(child(project, "dependencies"), "dependency");
    }

    /** dependencyManagement 中 scope=import 的 BOM。 */
    private static List<String> importedBoms(Element project) {
        return children(child(child(project, "dependencyManagement"), "dependencies"), "dependency").stream()
                .filter(dependency -> child(dependency, "scope") != null
                        && "import".equals(childText(dependency, "scope")))
                .map(dependency -> childText(dependency, "groupId") + ":" + childText(dependency, "artifactId"))
                .toList();
    }

    /** 由測試類別輸出目錄（{@code <module>/target/test-classes}）推回 repo 根目錄。 */
    private static Path repoRoot() throws Exception {
        Path testClasses = Path.of(UpgradeGovernanceContractTest.class
                .getProtectionDomain().getCodeSource().getLocation().toURI());
        Path root = testClasses.getParent().getParent().getParent();
        assertThat(root.resolve("pom.xml")).isRegularFile();
        return root;
    }
}
