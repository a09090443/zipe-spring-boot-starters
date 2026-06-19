package com.zipe.util.classloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

/**
 * 自訂類別載入器，繼承自 {@link URLClassLoader}，支援從外部 JAR 動態載入類別。
 *
 * <p>載入策略遵循雙親委派模型（Parent Delegation Model）：
 * 優先委派父類別載入器處理，若父載入器無法找到指定類別，
 * 才改由本載入器從已註冊的 URL（外部 JAR）中尋找並載入。
 * 此設計可避免重複載入 JVM 內建或應用程式已載入的類別。</p>
 *
 * <p>亦提供 {@link #unloadJarFile(URL)} 方法，用於釋放已開啟的 JAR 資源，
 * 適合需要熱替換（hot-swap）JAR 的應用情境。</p>
 */
public class CustomClassLoader extends URLClassLoader {

    /**
     * 建立自訂類別載入器實例。
     *
     * @param urls   要搜尋類別的 URL 陣列，通常為外部 JAR 檔案的路徑
     * @param parent 父類別載入器，用於雙親委派機制
     */
    public CustomClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * 載入指定名稱的類別。
     *
     * <p>優先委派父類別載入器處理；若父載入器拋出 {@link ClassNotFoundException}，
     * 則改呼叫 {@link #findClass(String)} 從已註冊的 URL（外部 JAR）中搜尋類別。</p>
     *
     * @param name 欲載入的類別完整名稱（fully qualified name），例如 {@code com.example.MyClass}
     * @return 已載入的 {@link Class} 物件
     * @throws ClassNotFoundException 若父載入器與本載入器均無法找到指定類別時拋出
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            //嘗試使用父類載入器載入類
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
            // 如果父類載入器沒有載入該類，則嘗試從外部 JAR 載入
            return findClass(name);
        }
    }

    /**
     * 釋放指定 URL 對應的 JAR 檔案資源。
     *
     * <p>遍歷目前已註冊的 URL 清單，找到與 {@code jarUrl} 相符的項目後
     * 開啟對應的 {@link JarFile} 並關閉，以釋放作業系統層級的檔案控制代碼（file handle）。
     * 此操作在熱替換 JAR 前必須執行，否則在 Windows 等系統上檔案仍會被鎖定。</p>
     *
     * @param jarUrl 欲卸載的 JAR 檔案 URL
     * @throws IOException 若關閉 JAR 檔案時發生 I/O 錯誤
     */
    public void unloadJarFile(URL jarUrl) throws IOException {
        try {
            for (URL url : this.getURLs()) {
                if (url.equals(jarUrl)) {
                    JarFile jarFile = new JarFile(new File(url.getFile()));
                    jarFile.close();
                    break;
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to unload JAR file", e);
        }
    }

}
