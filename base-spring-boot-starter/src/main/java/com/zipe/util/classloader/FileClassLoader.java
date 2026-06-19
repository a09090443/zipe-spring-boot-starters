package com.zipe.util.classloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;

/**
 * 從檔案系統動態載入並執行指定的 .class 檔案。
 *
 * <p>繼承 {@link ClassLoader}，覆寫 {@link #findClass(String)} 以從指定目錄讀取
 * 位元組碼並透過 {@link #defineClass} 完成類別定義，適用於需要在執行期動態替換或
 * 載入尚未在 classpath 中的類別之場景。</p>
 *
 * @Description 讀取並執行指定的 Class file
 */
public class FileClassLoader extends ClassLoader {

    /** 存放 .class 檔案的根目錄路徑 */
    private String classPath;

    /**
     * 建立不指定根目錄的 FileClassLoader；
     * 使用前須透過其他管道提供類別路徑，否則無法自行定位 .class 檔案。
     */
    public FileClassLoader() {
    }

    /**
     * 建立指定根目錄的 FileClassLoader。
     *
     * @param classPath 存放 .class 檔案的目錄絕對路徑（例如 {@code "D:/tmp/"}）
     */
    public FileClassLoader(String classPath) {
        this.classPath = classPath;
    }

    /**
     * 依類別全名從 {@code classPath} 目錄讀取對應的 .class 檔案並定義類別。
     *
     * <p>若讀取過程發生 {@link IOException}，會先列印堆疊追蹤，
     * 再委派給父類別 {@link ClassLoader#findClass(String)} 繼續嘗試載入。</p>
     *
     * @param name 類別全名（含套件名稱，例如 {@code "com.example.Foo"}）
     * @return 已定義的 {@link Class} 物件
     * @throws ClassNotFoundException 若父類別也無法找到對應類別時拋出
     */
    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        String fileName = getFileName(name);
        File file = new File(classPath, fileName);
        try {
            FileInputStream is = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int len = 0;
            try {
                // 逐位元組讀取 .class 檔案內容並寫入緩衝區
                while ((len = is.read()) != -1) {
                    bos.write(len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            byte[] data = bos.toByteArray();
            is.close();
            bos.close();
            // 將原始位元組陣列轉換為 JVM 可識別的 Class 物件
            return defineClass(name, data, 0, data.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 本地讀取失敗時，回退至父類別的尋找機制
        return super.findClass(name);
    }

    /**
     * 從已開啟的 {@link FileInputStream} 讀取位元組碼並定義類別。
     *
     * <p>此多載方法適合外部已開啟檔案串流的情境，由呼叫端負責關閉串流。</p>
     *
     * @param name 類別全名（含套件名稱）
     * @param fis  已開啟並指向 .class 檔案的輸入串流
     * @return 已定義的 {@link Class} 物件
     * @throws IOException 若讀取串流過程發生 I/O 錯誤時拋出
     */
    public Class<?> findClass(String name, FileInputStream fis) throws IOException {
        byte[] b = loadClassData(fis);
        return defineClass(name, b, 0, b.length);
    }

    /**
     * 將類別全名轉換為對應的 .class 檔案名稱。
     *
     * <p>若名稱包含套件分隔符（{@code .}），則僅取最後一段作為簡單類別名稱；
     * 例如 {@code "com.example.Foo"} 會回傳 {@code "Foo.class"}。</p>
     *
     * @param name 類別全名
     * @return 對應的 .class 檔案名稱
     */
    private String getFileName(String name) {
        int index = name.lastIndexOf(".");
        if (index == -1) {
            // 不含套件前綴，直接附加副檔名
            return name + ".class";
        } else {
            // 取最後一個 '.' 之後的簡單類別名稱，避免將套件路徑誤認為檔名
            return name.substring(index + 1) + ".class";
        }
    }

    /**
     * 從 {@link FileInputStream} 一次性讀取完整的 .class 位元組陣列。
     *
     * <p>若實際讀取位元組數與檔案大小不符，表示讀取不完整，會拋出 {@link IOException}
     * 以避免載入損毀的類別定義。</p>
     *
     * @param fis 已開啟並指向 .class 檔案的輸入串流
     * @return 包含完整 .class 位元組碼的陣列
     * @throws IOException 若讀取位元組數不符預期時拋出
     */
    private byte[] loadClassData(FileInputStream fis) throws IOException {
        byte[] buffer = new byte[fis.available()];
        int bytesRead = fis.read(buffer);
        // 驗證讀取完整性，防止因串流提前結束而載入不完整的類別
        if (bytesRead != buffer.length) {
            throw new IOException("Could not read the entire file: " + bytesRead + " bytes read, but expected " + buffer.length);
        }
        return buffer;
    }

    /**
     * 示範如何使用 FileClassLoader 動態載入類別並透過反射呼叫方法。
     *
     * <p>此為手動測試用進入點，示範從 {@code D:/tmp/} 載入
     * {@code tw.com.webcomm.util.WebServiceHandler} 並執行其 {@code test} 方法。</p>
     *
     * @param args 命令列引數（本示範中未使用）
     * @throws MalformedURLException 若 URL 格式不正確時拋出（本示範中實際不觸發）
     */
    public static void main(String[] args) throws MalformedURLException {

        FileClassLoader diskClassLoader = new FileClassLoader("D:/tmp/");
        try {
            Class<?> c = diskClassLoader.loadClass("tw.com.webcomm.util.WebServiceHandler");
            if (c != null) {
                Object obj = c.getDeclaredConstructor().newInstance();
                Method method = c.getDeclaredMethod("test", String.class);
                method.invoke(obj, "I'm back");
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }

    }
}
