package com.zipe.compat;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.w3c.dom.Element;

/**
 * Spring Boot 版本一致性測試：測試執行期實際載入的 Spring Boot 版本必須等於根 pom
 * {@code spring-boot-starter-parent} 的版本，且為確切的 {@code 4.0.N}（N≥1）。
 *
 * <p>本檔放在 base（無內部相依）與 iam（最深相依鏈 iam→logon→base）兩個模組，內容相同，
 * 代表 7 個 reactor starter 的邊界。根 pom 為 Boot 版本的唯一來源，子模組不得另行覆寫。</p>
 */
class SpringBootVersionAlignmentTest {

    /** 允許的版本格式：確切的 4.0.N 且 N≥1（不接受 4.0.0、版本範圍或其他 minor）。 */
    private static final String PATCH_VERSION_PATTERN = "^4\\.0\\.[1-9]\\d*$";

    @Test
    void runtimeBootVersionMatchesRootParent() throws Exception {
        String parentVersion = rootParentVersion();

        assertThat(parentVersion).matches(PATCH_VERSION_PATTERN);
        assertThat(SpringBootVersion.getVersion()).isEqualTo(parentVersion);
        assertThat(SpringApplication.class.getPackage().getImplementationVersion()).isEqualTo(parentVersion);
    }

    /** 解析根 pom（模組目錄的上一層）中 {@code spring-boot-starter-parent} 的版本。 */
    private static String rootParentVersion() throws Exception {
        File rootPom = moduleDir().resolve("../pom.xml").normalize().toFile();
        assertThat(rootPom).isFile();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Element project = factory.newDocumentBuilder().parse(rootPom).getDocumentElement();
        Element parent = (Element) project.getElementsByTagName("parent").item(0);

        assertThat(text(parent, "groupId")).isEqualTo("org.springframework.boot");
        assertThat(text(parent, "artifactId")).isEqualTo("spring-boot-starter-parent");
        return text(parent, "version");
    }

    private static String text(Element element, String tag) {
        return element.getElementsByTagName(tag).item(0).getTextContent().trim();
    }

    /** 由測試類別輸出目錄（{@code target/test-classes}）推回模組目錄。 */
    private static Path moduleDir() throws Exception {
        Path testClasses = Path.of(SpringBootVersionAlignmentTest.class
                .getProtectionDomain().getCodeSource().getLocation().toURI());
        return testClasses.getParent().getParent();
    }
}
