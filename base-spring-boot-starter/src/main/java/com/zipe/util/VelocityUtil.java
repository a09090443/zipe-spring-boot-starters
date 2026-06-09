package com.zipe.util;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * Apache Velocity 樣板引擎工具類別。
 * <p>
 * 封裝 {@link VelocityEngine} 的初始化與樣板渲染邏輯，支援四種資源載入方式：
 * <ul>
 *   <li>Classpath 路徑載入（{@link #initClassPath()}）</li>
 *   <li>檔案系統相對路徑載入（{@link #initFilePath()}）</li>
 *   <li>指定絕對路徑載入（{@link #initFileSystemPath(String)}）</li>
 *   <li>Web 應用程式路徑載入（{@link #initWebPath(HttpServletRequest, String)}）</li>
 * </ul>
 * 使用前請先呼叫其中一個 {@code init*} 方法完成初始化，
 * 再透過 {@link #generateContent(String, Map)} 或 {@link #writeTemplateOutput(String, String, Map)} 進行樣板渲染。
 * 使用完畢後可呼叫 {@link #close()} 釋放引擎參考。
 *
 * @author : Gary Tsai
 * @created : @Date 2020/11/24 下午 05:39
 **/
public class VelocityUtil {
    /** Velocity 引擎實例，透過 init* 方法完成初始化後才可使用。 */
    private VelocityEngine ve;
    /** Web 應用程式路徑模式所需的 HTTP 請求物件，用於取得 ServletContext。 */
    private HttpServletRequest request;
    /** 樣板資源的預設根路徑，可透過 {@link #setResourcePath(String)} 設定。 */
    private String resourcePath;
    /** 樣板檔案所在的子目錄，會自動拼接至樣板名稱前；可透過 {@link #setDir(String)} 設定。 */
    private String dir;

    /**
     * 建立 VelocityUtil 實例，使用預設無參數建構子。
     * 建立後須呼叫 {@code init*} 系列方法之一完成初始化，方可渲染樣板。
     */
    public VelocityUtil() {
    }

    /**
     * 使用 Classpath 資源載入器初始化 Velocity 引擎。
     * <p>
     * 樣板檔案須放置於應用程式的 Classpath 中（例如 {@code src/main/resources/}）。
     */
    public void initClassPath() {
        Properties p = new Properties();
        p.put("resource.loader", "classpath");
        p.put("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        this.init(p);
    }

    /**
     * 使用檔案系統資源載入器初始化 Velocity 引擎（不指定根路徑）。
     * <p>
     * 適用於以相對路徑參照樣板檔案的場景；若需指定根目錄，請改用 {@link #initFileSystemPath(String)}。
     */
    public void initFilePath() {
        Properties p = new Properties();
        p.put("resource.loader", "file");
        p.put("file.resource.loader.class", FileResourceLoader.class.getName());
        this.init(p);
    }

    /**
     * 使用檔案系統資源載入器並指定根路徑初始化 Velocity 引擎。
     * <p>
     * 路徑優先順序：
     * <ol>
     *   <li>若 {@code templateFilePath} 非空，使用該路徑。</li>
     *   <li>否則若 {@link #resourcePath} 已設定，使用 {@code resourcePath}。</li>
     *   <li>以上皆未設定時，預設使用 {@code "tempalte/"}。</li>
     * </ol>
     *
     * @param templateFilePath 樣板檔案的根目錄路徑；傳入 {@code null} 或空字串時將自動回退至預設值
     */
    public void initFileSystemPath(String templateFilePath) {
        Properties p = new Properties();
        p.put("resource.loader", "file");
        // 依優先順序決定樣板根路徑：參數 > resourcePath > 預設值
        if (null != templateFilePath && !"".equals(templateFilePath)) {
            p.put("file.resource.loader.path", templateFilePath);
        } else {
            p.put("file.resource.loader.path", null != this.resourcePath && !"".equals(this.resourcePath) ? this.resourcePath : "tempalte/");
        }

        this.init(p);
    }

    /**
     * 使用 Web 應用程式資源載入器初始化 Velocity 引擎。
     * <p>
     * 此模式會將 {@link javax.servlet.ServletContext} 注入至引擎，以便解析 WEB-INF 下的樣板。
     * 路徑優先順序與 {@link #initFileSystemPath(String)} 相同，預設值為 {@code "/WEB-INF/template/"}。
     *
     * @param request          目前的 HTTP 請求，用於取得 {@link javax.servlet.ServletContext}
     * @param templateFilePath 樣板檔案的 Web 根路徑；傳入 {@code null} 或空字串時將自動回退至預設值
     */
    public void initWebPath(HttpServletRequest request, String templateFilePath) {
        this.request = request;
        Properties p = new Properties();
        p.put("resource.loader", "webapp");
        p.put("webapp.resource.loader.class", "org.apache.velocity.tools.view.servlet.WebappLoader");
        // 依優先順序決定 Web 樣板根路徑：參數 > resourcePath > 預設值
        if (null != templateFilePath && !"".equals(templateFilePath)) {
            p.put("webapp.resource.loader.path", templateFilePath);
        } else {
            p.put("webapp.resource.loader.path", null != this.resourcePath && !"".equals(this.resourcePath) ? this.resourcePath : "/WEB-INF/template/");
        }

        this.init(p);
    }

    /**
     * 將渲染後的樣板內容寫入指定的輸出檔案（UTF-8 編碼）。
     * <p>
     * 使用 1 MB（1,048,576 bytes）的緩衝區以提升寫入效率。
     *
     * @param templateName 樣板檔案名稱（相對於資源根路徑）
     * @param outputFile   輸出檔案的完整路徑（含檔名）
     * @param map          注入樣板的鍵值對應資料
     * @throws Exception 樣板渲染失敗或寫入檔案發生 I/O 錯誤時拋出
     */
    public void writeTemplateOutput(String templateName, String outputFile, Map<String, Object> map) throws Exception {
        try {
            BufferedWriter bw = null;

            try {
                // 使用 1 MB 緩衝區，以 UTF-8 編碼寫出渲染結果
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8.name()), 1048576);
                bw.write(this.generateContent(templateName, map));
                bw.flush();
            } catch (Exception var10) {
                throw var10;
            } finally {
                if (bw != null) {
                    bw.close();
                }

            }

        } catch (Exception var12) {
            throw var12;
        }
    }

    /**
     * 渲染指定的 Velocity 樣板並以字串形式回傳結果。
     * <p>
     * 若 {@link #dir} 已設定，會自動在樣板名稱前加上目錄分隔符號與目錄名稱。
     * 方法會將 {@code map} 中所有鍵值對注入至 {@link VelocityContext}，再執行樣板合併。
     *
     * @param templateName 樣板檔案名稱（相對於資源根路徑，不含目錄前綴）
     * @param map          注入樣板的鍵值對應資料
     * @return 渲染完成的字串內容
     * @throws RuntimeException 樣板載入或渲染過程發生例外時重新拋出
     */
    public String generateContent(String templateName, Map<String, Object> map) {
        StringWriter writer = new StringWriter();
        // 若有設定子目錄，將目錄前綴拼接至樣板名稱
        if (null != this.dir && "" != this.dir) {
            templateName = this.dir + File.separator + templateName;
        }

        try {
            Template template = this.ve.getTemplate(templateName);
            VelocityContext context = new VelocityContext();
            Set<String> keys = map.keySet();
            Iterator var7 = keys.iterator();

            // 將 map 中所有鍵值逐一放入 VelocityContext，供樣板中的 $variable 參照
            while (var7.hasNext()) {
                String key = (String) var7.next();
                context.put(key, map.get(key));
            }

            template.merge(context, writer);
            return writer.toString();
        } catch (Exception var9) {
            throw var9;
        }
    }

    /**
     * 以指定的 Properties 初始化 {@link VelocityEngine}。
     * <p>
     * 強制設定輸入與輸出編碼為 UTF-8。
     * 此方法僅在 {@code ve} 尚未初始化時才會建立引擎實例，避免重複初始化。
     * 若為 Web 模式（{@link #request} 不為 {@code null}），
     * 會將 {@link javax.servlet.ServletContext} 注入引擎以供 WebappLoader 使用。
     *
     * @param p 包含資源載入器設定的 Properties 物件
     */
    private void init(Properties p) {
        // 確保輸入與輸出一律使用 UTF-8，避免樣板內容亂碼
        p.put("input.encoding", StandardCharsets.UTF_8.name());
        p.put("output.encoding", StandardCharsets.UTF_8.name());
        // 使用懶初始化（lazy init）：只在尚未建立引擎時才進行初始化
        if (null == this.ve) {
            this.ve = new VelocityEngine(p);
            if (!Objects.isNull(this.request)) {
                // Web 模式需將 ServletContext 注入引擎，WebappLoader 才能正確解析樣板路徑
                this.ve.setApplicationAttribute("javax.servlet.ServletContext", this.request.getSession().getServletContext());
            }

            this.ve.init();
        }

    }

    /**
     * 釋放 Velocity 引擎實例，允許 GC 回收資源。
     * <p>
     * 若需重新以不同設定初始化引擎，可先呼叫此方法再呼叫對應的 {@code init*} 方法。
     */
    public void close() {
        if (null != this.ve) {
            this.ve = null;
        }

    }

    /**
     * 用於本地開發測試的程式進入點（已停用）。
     *
     * @param args 命令列引數（未使用）
     */
    public static void main(String[] args) {
//        VelocityUtil createHtml = new VelocityUtil();
//        Map<String, Object> map = new HashMap();
//        map.put("name", "Gary Tsai");
//
//        try {
//            createHtml.writeTemplateOutput("template/hello.vm", "/home/zipe/tmp/test.html", map);
//        } catch (Exception var4) {
//            var4.printStackTrace();
//        }

    }

    /**
     * 取得樣板資源的預設根路徑。
     *
     * @return 目前設定的資源根路徑，若未設定則為 {@code null}
     */
    public String getResourcePath() {
        return this.resourcePath;
    }

    /**
     * 設定樣板資源的預設根路徑。
     * <p>
     * 須在呼叫 {@code init*} 方法之前設定，方可生效。
     *
     * @param resourcePath 樣板資源的根路徑
     */
    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    /**
     * 取得樣板檔案所在的子目錄名稱。
     *
     * @return 目前設定的子目錄名稱，若未設定則為 {@code null}
     */
    public String getDir() {
        return this.dir;
    }

    /**
     * 設定樣板檔案所在的子目錄名稱。
     * <p>
     * 設定後，{@link #generateContent(String, Map)} 會自動將此目錄前綴拼接至樣板名稱。
     *
     * @param dir 樣板子目錄名稱
     */
    public void setDir(String dir) {
        this.dir = dir;
    }
}
