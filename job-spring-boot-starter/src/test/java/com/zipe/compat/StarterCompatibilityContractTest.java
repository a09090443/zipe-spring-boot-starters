package com.zipe.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

/**
 * Starter 對外相容面契約測試：本模組註冊的自動配置清單與設定屬性鍵，必須與升級前的固定基準完全相同。
 *
 * <p>基準檔位於 {@code src/test/resources/compat-baseline/}，由升級前的原始碼產生後固定，
 * 不得在測試時由待驗程式重建：</p>
 * <ul>
 *   <li>{@code autoconfiguration-imports.txt}：{@code AutoConfiguration.imports} 的類別集合（排序、每行一筆）；</li>
 *   <li>{@code configuration-properties.txt}：本模組全部 {@code @ConfigurationProperties} 展開後的屬性鍵
 *       （類別層級為 {@code prefix.key}；{@code @Bean} 方法層級記為 {@code @Bean prefix -> 回傳型別}）。</li>
 * </ul>
 *
 * <p>本檔在<b>每個 {@code *-spring-boot-starter} reactor 模組各放一份，內容相同</b>（比照
 * {@code StarterArchitectureTest}），只掃描本模組自己的 {@code target/classes}，不含相依模組。
 * 僅使用 JUnit 與 Spring 核心 API，不需額外測試相依。任何新增、刪除或改名都會使測試失敗；
 * 若屬刻意的公開行為變更，須同步更新基準檔與文件。</p>
 */
class StarterCompatibilityContractTest {

    /** 自動配置註冊檔在 classes 目錄下的相對路徑。 */
    private static final String IMPORTS_PATH =
            "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

    /** 巢狀屬性展開的最大深度，防止型別互相參照造成無限遞迴。 */
    private static final int MAX_DEPTH = 6;

    @Test
    void autoConfigurationImportsMatchBaseline() throws Exception {
        Path importsFile = mainClassesDir().resolve(IMPORTS_PATH);
        assertTrue(Files.isRegularFile(importsFile), "本模組必須有 AutoConfiguration.imports");

        Set<String> actual = readEntries(Files.readAllLines(importsFile, StandardCharsets.UTF_8));
        Set<String> expected = baseline("autoconfiguration-imports.txt");

        assertSameSet("AutoConfiguration.imports", actual, expected);
        assertFalse(expected.isEmpty(), "基準檔不可為空");
    }

    @Test
    void configurationPropertyKeysMatchBaseline() throws Exception {
        Set<String> actual = scanConfigurationPropertyKeys(mainClassesDir());
        Set<String> expected = baseline("configuration-properties.txt");

        assertSameSet("@ConfigurationProperties 屬性鍵", actual, expected);
        assertFalse(expected.isEmpty(), "基準檔不可為空");
    }

    private static void assertSameSet(String label, Set<String> actual, Set<String> expected) {
        Set<String> added = new TreeSet<>(actual);
        added.removeAll(expected);
        Set<String> removed = new TreeSet<>(expected);
        removed.removeAll(actual);
        assertEquals(expected, actual, () -> String.format(
                "%s 與升級前基準不一致%n新增：%s%n缺少：%s%n目前完整集合：%n%s",
                label, added, removed, String.join(System.lineSeparator(), actual)));
    }

    // ------------------------------------------------------------------ 屬性鍵掃描

    private static Set<String> scanConfigurationPropertyKeys(Path classesDir) throws IOException {
        MetadataReaderFactory readerFactory = new SimpleMetadataReaderFactory();
        String annotation = ConfigurationProperties.class.getName();
        Set<String> keys = new TreeSet<>();
        List<Path> classFiles;
        try (Stream<Path> files = Files.walk(classesDir)) {
            classFiles = files.filter(p -> p.toString().endsWith(".class")).sorted().collect(Collectors.toList());
        }
        for (Path classFile : classFiles) {
            AnnotationMetadata metadata =
                    readerFactory.getMetadataReader(new FileSystemResource(classFile)).getAnnotationMetadata();
            MergedAnnotation<ConfigurationProperties> onClass =
                    metadata.getAnnotations().get(ConfigurationProperties.class);
            if (onClass.isDirectlyPresent()) {
                String prefix = onClass.getString("prefix");
                collectKeys(prefix, loadClass(metadata.getClassName()), keys, 0);
            }
            for (MethodMetadata method : metadata.getAnnotatedMethods(annotation)) {
                String prefix = method.getAnnotations().get(ConfigurationProperties.class).getString("prefix");
                keys.add("@Bean " + prefix + " -> " + method.getReturnTypeName());
            }
        }
        return keys;
    }

    private static void collectKeys(String prefix, Class<?> type, Set<String> keys, int depth) {
        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                addKey(prefix, component.getName(),
                        ResolvableType.forMethodReturnType(component.getAccessor()), keys, depth);
            }
            return;
        }
        for (PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(type)) {
            Method read = descriptor.getReadMethod();
            if ("class".equals(descriptor.getName()) || read == null) {
                continue;
            }
            ResolvableType propertyType = ResolvableType.forMethodReturnType(read);
            boolean nestedBean = isNestedBean(propertyType.resolve(Object.class));
            if (descriptor.getWriteMethod() == null && !nestedBean) {
                continue;
            }
            addKey(prefix, descriptor.getName(), propertyType, keys, depth);
        }
    }

    private static void addKey(String prefix, String name, ResolvableType type, Set<String> keys, int depth) {
        String key = prefix + "." + toDashedForm(name);
        Class<?> raw = type.resolve(Object.class);
        ResolvableType element = null;
        String suffix = "";
        if (raw.isArray()) {
            element = type.getComponentType();
            suffix = "[*]";
        } else if (Iterable.class.isAssignableFrom(raw)) {
            element = type.as(Iterable.class).getGeneric(0);
            suffix = "[*]";
        } else if (Map.class.isAssignableFrom(raw)) {
            element = type.asMap().getGeneric(1);
            suffix = ".*";
        }
        Class<?> target = element != null ? element.resolve(Object.class) : raw;
        if (isNestedBean(target) && depth < MAX_DEPTH) {
            collectKeys(key + suffix, target, keys, depth + 1);
        } else {
            keys.add(key + suffix);
        }
    }

    /** 本專案（{@code com.zipe}）的非列舉、非介面型別視為巢狀設定物件，繼續展開。 */
    private static boolean isNestedBean(Class<?> type) {
        return type.getName().startsWith("com.zipe.") && !type.isEnum() && !type.isInterface()
                && !type.isPrimitive();
    }

    private static String toDashedForm(String name) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    result.append('-');
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className, false, StarterCompatibilityContractTest.class.getClassLoader());
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("無法載入設定屬性類別 " + className, ex);
        }
    }

    // ------------------------------------------------------------------ 共用工具

    private static Set<String> baseline(String fileName) throws IOException {
        String resource = "compat-baseline/" + fileName;
        try (InputStream in = StarterCompatibilityContractTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "缺少基準檔 src/test/resources/" + resource);
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return readEntries(content.lines().collect(Collectors.toList()));
        }
    }

    /** 讀取非空白、非 {@code #} 註解的行並去除前後空白。 */
    private static Set<String> readEntries(List<String> lines) {
        return lines.stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** 本模組主程式碼的輸出目錄：由 {@code target/test-classes} 推回同層的 {@code target/classes}。 */
    private static Path mainClassesDir() throws Exception {
        Path testClasses = Path.of(StarterCompatibilityContractTest.class
                .getProtectionDomain().getCodeSource().getLocation().toURI());
        Path classes = testClasses.resolveSibling("classes");
        assertTrue(Files.isDirectory(classes), "找不到本模組的 target/classes");
        return classes;
    }
}
