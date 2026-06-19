package com.zipe.util.classloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * 自訂 JAR 類別載入器，繼承自 {@link URLClassLoader}。
 *
 * <p>允許在執行期間動態載入指定 JAR 檔案中的類別。
 * 透過 {@link #addAppliedPackages(String)} 可指定特定套件名稱，
 * 符合條件的類別將直接從 JAR 的位元組流中載入，
 * 不走標準雙親委派機制，以確保使用 JAR 內的版本而非已載入至 JVM 的版本。</p>
 *
 * @Description 讀取並執行指定的 Jar file
 */
public class JarClassLoader extends URLClassLoader {
    /** 需要強制從 JAR 直接載入的套件名稱清單 */
    private List<String> appliedPackages;

    /**
     * 以指定的 URL 陣列建立 JarClassLoader 實例。
     *
     * @param urls 要搜尋類別的 JAR 檔案 URL 陣列，通常以 {@code file:} 開頭
     */
    public JarClassLoader(URL[] urls) {
        super(urls);
    }

    /**
     * 取得需要強制從 JAR 載入的套件名稱清單，若尚未初始化則建立空清單。
     *
     * @return 套件名稱清單
     */
    private List<String> getAppliedPackages() {
        // 延遲初始化，避免不必要的記憶體配置
        if (appliedPackages == null) {
            this.appliedPackages = new ArrayList<String>();
        }
        return this.appliedPackages;
    }

    /**
     * 新增一個需要強制從 JAR 直接載入的套件名稱。
     *
     * <p>符合此套件的類別將繞過雙親委派機制，
     * 直接從 JAR 位元組流中定義，確保使用 JAR 內包含的版本。</p>
     *
     * @param packageName 套件名稱前綴，例如 {@code org.example}
     */
    public void addAppliedPackages(String packageName) {
        getAppliedPackages().add(packageName);
    }

    /**
     * 判斷指定類別名稱是否屬於需要強制從 JAR 載入的套件。
     *
     * @param className 完整類別名稱（含套件路徑）
     * @return 若類別名稱包含任一已登錄的套件前綴，回傳 {@code true}；否則回傳 {@code false}
     */
    private boolean applyFixedLoad(String className) {
        for (String packageName : getAppliedPackages()) {
            if (className.contains(packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 在已登錄的 JAR 檔案中搜尋並載入指定類別。
     *
     * <p>若類別名稱符合強制載入的套件，則逐一掃描所有 JAR URL，
     * 從中讀取 {@code .class} 位元組並呼叫 {@link #defineClass} 定義類別；
     * 否則委派給父類別 {@link URLClassLoader#findClass} 處理。</p>
     *
     * @param name 要尋找的完整類別名稱
     * @return 找到的 {@link Class} 物件，若未找到則回傳 {@code null}（強制載入情境）或拋出例外
     * @throws ClassNotFoundException 委派給父類別時若找不到類別則拋出此例外
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (applyFixedLoad(name)) {
            // 將類別名稱的點號轉換為路徑分隔符，並附加 .class 副檔名
            // construct path with .class file
            String classFile = name.replaceAll("\\.", "/") + ".class";
            // Read all entries of defined JARs and try to found given class
            URL[] urls = getURLs();
            int i = 0;
            Class<?> searchedClass = null;
            while (searchedClass == null && i < urls.length) {
                URL url = urls[i];
                i++;
                JarFile jarFile = null;
                try {
                    /**
                     * JarFile is constructed directly from File instance. After
                     * we get ZipEntry which is, in fact, the element composing the JAR.
                     * If entry is found, we construct class instance from read bytes.
                     * The bytes are read in readClassBytesFromFile method.
                     */
                    File jar = new File(url.getFile());
                    try {
                        jarFile = new JarFile(jar);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    ZipEntry entry = jarFile.getEntry(classFile);
                    if (entry != null) {
                        byte[] entryBytes = readClassBytesFromFile(jar, classFile);
                        searchedClass = defineClass(name, entryBytes, 0, entryBytes.length);
                    }
                } finally {
                    if (jarFile != null) {
                        try {
                            jarFile.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return searchedClass;
        }
        return super.findClass(name);
    }

    /**
     * Reads bytes from given JAR file. The reading is based on special URL implementation
     * dedicated to JARs access. It starts by jar: protocol and continues with the name of the file.
     * The final !/ separates this two parts from the name of searched class. For example,
     * this URL jar:file://home/bartosz/tmp/jars/spring-core-4.0.0.RELEASE.jar!/org/springframework/util/PropertyPlaceholderHelper.class
     * - jar: marks the URL as JAR URL
     * - file://home/bartosz/tmp/jars/spring-core-4.0.0.RELEASE.jar: address of the JAR file containing searched class
     * - !/org/springframework/util/PropertyPlaceholderHelper.class: the name of searched class
     *
     * @param jar       File containing searched class.
     * @param className Searched class.
     * @return Array of bytes with searched class (if error, the array is empty).
     */
    private byte[] readClassBytesFromFile(File jar, String className) {
        InputStream stream = null;
        try {
            URL urlTmp = new URL("jar:" + getURI(jar) + "!/" + className);
            JarURLConnection jarUrl = (JarURLConnection) urlTmp.openConnection();
            stream = jarUrl.getInputStream();
            byte[] bytes = new byte[stream.available()];
            stream.read(bytes);
            return bytes;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 載入指定類別，並視需要進行連結（resolve）。
     *
     * <p>若類別名稱符合強制載入的套件，則直接呼叫 {@link #findClass} 從 JAR 中載入，
     * 跳過標準雙親委派；否則委派給父類別的 {@link URLClassLoader#loadClass} 處理。
     * 此方法宣告為 {@code synchronized} 以確保多執行緒環境下的載入安全性。</p>
     *
     * @param name    要載入的完整類別名稱
     * @param resolve 若為 {@code true}，載入後立即進行連結階段（resolve）
     * @return 載入完成的 {@link Class} 物件
     * @throws ClassNotFoundException 若指定類別無法找到則拋出此例外
     */
    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (applyFixedLoad(name)) {
            Class<?> searchedClass = findClass(name);
            // 若呼叫端要求連結，則在回傳前執行 resolve 以完成符號解析
            if (resolve) {
                resolveClass(searchedClass);
            }
            return searchedClass;
        }
        return super.loadClass(name, resolve);
    }


    /**
     * 將 {@link File} 轉換為標準化的 {@link URL}，用於組合 JAR URL。
     *
     * <p>先取得 canonical 路徑以解析符號連結及相對路徑，
     * 再轉換為 URI/URL，確保路徑格式正確。</p>
     *
     * <p>Taken from Tomcat7 class loader: org.apache.catalina.loader.WebappClassLoader</p>
     *
     * @param file 要轉換的 JAR 檔案
     * @return 對應的 {@link URL}
     * @throws MalformedURLException 若 URI 無法轉換為合法 URL 則拋出此例外
     * @throws IOException           若取得 canonical 路徑時發生 I/O 錯誤則拋出此例外
     */
    private URL getURI(File file) throws MalformedURLException, IOException {
        File realFile = file;
        // 取得 canonical 路徑以正規化符號連結與相對路徑
        realFile = realFile.getCanonicalFile();
        return realFile.toURI().toURL();
    }

    /**
     * 手動測試入口，示範如何使用 JarClassLoader 動態載入 JAR 中的類別並反射呼叫方法。
     *
     * @param args 命令列參數（未使用）
     * @throws MalformedURLException     若 JAR 路徑 URL 格式錯誤則拋出此例外
     * @throws ClassNotFoundException    若找不到指定類別則拋出此例外
     * @throws NoSuchMethodException     若找不到指定方法則拋出此例外
     * @throws InvocationTargetException 若反射呼叫的方法本身拋出例外則包裝拋出此例外
     * @throws InstantiationException    若無法實例化類別則拋出此例外
     * @throws IllegalAccessException    若無存取權限則拋出此例外
     */
    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        JarClassLoader loader = new JarClassLoader(new URL[]{
                new URL("file:D:\\tmp\\classloaderTest.jar")
        });
        loader.addAppliedPackages("org.example");
        String searchedClass = "org.example.Test2";
        Class<?> c = loader.loadClass(searchedClass);
        if (c != null) {
            Object obj = c.getDeclaredConstructor().newInstance();
            Method method = c.getDeclaredMethod("getValue", String.class);
            method.invoke(obj, "3213");
        }
    }
}
