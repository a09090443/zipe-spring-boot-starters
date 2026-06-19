package com.zipe.util.string;

/**
 * 字串常數工具類別。
 *
 * <p>集中定義系統各處共用的字串常數，涵蓋以下分類：
 * <ul>
 *   <li>字元編碼（MS950、Big5、UTF-8 等）</li>
 *   <li>分頁相關預設值</li>
 *   <li>批次處理狀態描述</li>
 *   <li>常用標點符號與特殊字元</li>
 *   <li>是/否、成功/失敗等語意旗標</li>
 *   <li>換行符號（跨平台）</li>
 *   <li>HTTP 檔案下載相關 Header 值</li>
 *   <li>報表／文件樣板參數佔位符（_param=..._ 格式）</li>
 * </ul>
 *
 * <p>此類別僅供靜態常數存取，不應被繼承或實例化使用。
 */
public class StringConstant {

    // -------------------------------------------------------------------------
    // 系統識別
    // -------------------------------------------------------------------------

    /** 系統管理員帳號顯示名稱 */
    public static final String SYSTEM_ADMIN = "Admin";

    // -------------------------------------------------------------------------
    // 字元編碼
    // -------------------------------------------------------------------------

    /** MS950（繁體中文 Windows 編碼，相容 Big5）編碼名稱 */
    public static final String ENCODE_MS950 = "MS950";

    /** Big5 繁體中文編碼名稱 */
    public static final String ENCODE_BIG5 = "Big5";

    /** UTF-8 Unicode 編碼名稱 */
    public static final String ENCODE_UTF8 = "UTF-8";

    /**
     * Oracle SQL*Loader 使用的 UTF-8 編碼名稱。
     * Oracle 內部以 AL32UTF8 表示 UTF-8，與標準名稱不同。
     */
    public static final String SQLLOADER_ENCODE_UTF8 = "AL32UTF8";

    // -------------------------------------------------------------------------
    // 分頁相關預設值（可依業務需求覆寫）
    // -------------------------------------------------------------------------

    /** 預設起始頁碼（從第 0 頁開始） */
    public static int PAGE_INDEX = 0;

    /** 最大允許的分頁數量上限 */
    public static int MAX_PAGE_COUNT = 100;

    /** 每頁預設顯示的資料列數 */
    public static int PAGE_ROW_COUNT = 10;

    /** 分頁導覽列每次顯示的頁碼數量 */
    public static int PAGING = 10;

    // -------------------------------------------------------------------------
    // 批次處理狀態描述
    // -------------------------------------------------------------------------

    /** 批次作業已啟動的狀態描述文字 */
    public static final String BATCH_STATUS_DESC_START = "Start";

    /** 批次作業成功結束的狀態描述文字 */
    public static final String BATCH_STATUS_DESC_SUCCESS_END = "SuccessEnd";

    /** 批次作業失敗結束的狀態描述文字 */
    public static final String BATCH_STATUS_DESC_FAIL_END = "FailEnd";

    // -------------------------------------------------------------------------
    // 格式轉換狀態碼
    // -------------------------------------------------------------------------

    /** 格式轉換失敗的狀態碼（"3"） */
    public static final String CHANGE_FORMAT_STATUS_FAILED = "3";

    /** 格式轉換尚未完成的狀態碼（"0"） */
    public static final String CHANGE_FORMAT_STATUS_NOT_DONE = "0";

    // -------------------------------------------------------------------------
    // 常用標點符號與特殊字元
    // -------------------------------------------------------------------------

    /** 逗號 {@code ,} */
    public static final String COMMA = ",";

    /** 半形空白 */
    public static final String SPACE = " ";

    /** 冒號 {@code :} */
    public static final String COLON = ":";

    /** 百分比符號 {@code %} */
    public static final String PERCENT = "%";

    /** 句點 / 小數點 {@code .} */
    public static final String DOT = ".";

    /** 分號 {@code ;} */
    public static final String SEMICOLON = ";";

    /** 連字號（dash）{@code -} */
    public static final String DASH = "-";

    /** 底線 {@code _} */
    public static final String UNDERLINE = "_";

    /** At 符號 {@code @} */
    public static final String AT = "@";

    /** 等號 {@code =} */
    public static final String EQUALS = "=";

    /** 斜線 {@code /} */
    public static final String SLASH = "/";

    /** 波浪號 {@code ~} */
    public static final String TILDE = "~";

    /** 空字串（長度為 0 的字串） */
    public static final String BLANK = "";

    // -------------------------------------------------------------------------
    // 語意旗標（成功/失敗、是/否、真/假）
    // -------------------------------------------------------------------------

    /** 操作成功的小寫英文文字 */
    public static final String SUCCESS = "success";

    /** 操作失敗的小寫英文文字 */
    public static final String FAIL = "fail";

    /** 是的縮寫（單字元），常用於資料庫欄位旗標 */
    public static final String SHORT_YES = "Y";

    /** 否的縮寫（單字元），常用於資料庫欄位旗標 */
    public static final String SHORT_NO = "N";

    /** 是（完整英文大寫） */
    public static final String YES = "YES";

    /** 否（完整英文大寫） */
    public static final String NO = "NO";

    /** 真的縮寫（單字元） */
    public static final String SHORT_TRUE = "T";

    /** 假的縮寫（單字元） */
    public static final String SHORT_FALSE = "F";

    /** 真（完整英文大寫） */
    public static final String TRUE = "TRUE";

    /** 假（完整英文大寫） */
    public static final String FALSE = "FALSE";

    // -------------------------------------------------------------------------
    // 執行緒池
    // -------------------------------------------------------------------------

    /** 執行緒池預設執行緒數量 */
    public static final int DEFAULT_THREAD_SIZE = 5;

    // -------------------------------------------------------------------------
    // 換行符號
    // -------------------------------------------------------------------------

    /** Windows 換行符號（CRLF：{@code \r\n}） */
    public static final String LINE_SEPERATOR2 = "\r\n";

    /**
     * 目前作業系統的換行符號，由 JVM 於執行期取得。
     * Windows 為 {@code \r\n}，Unix/Linux/macOS 為 {@code \n}。
     */
    public static final String LINE_SEPERATOR = System.getProperty("line.separator");

    /** HTML 換行標籤，用於在網頁上顯示換行 */
    public static final String LINE_SEPERATOR3 = "<br/>";

    // -------------------------------------------------------------------------
    // 檔案與 HTTP 下載相關
    // -------------------------------------------------------------------------

    /** Excel 檔案副檔名（不含點） */
    public static final String EXCEL_SEXTENSION = "xlsx";

    /** HTTP 下載回應的 Content-Type：二進位串流 */
    public static final String SET_CONTENT_TYPE = "APPLICATION/OCTET-STREAM";

    /** HTTP Response Header 名稱：Set-Cookie */
    public static final String SET_HEADER_COOKIE = "Set-Cookie";

    /** 檔案下載用的 Cookie 路徑設定值 */
    public static final String SET_HEADER_FILEDOWNLOAD_PATH = "fileDownload=true; path=/";

    /** HTTP Response Header 名稱：Content-Disposition（控制瀏覽器下載行為） */
    public static final String SET_HEADER_CONTENT_DISPOSITION = "Content-disposition";

    /** Content-Disposition 附件下載前綴，後接檔案名稱 */
    public static final String SET_HEADER_ATTACHMENT_FILENAME = "attachment; filename=";

    // -------------------------------------------------------------------------
    // 報表／文件樣板參數佔位符
    // 用於在報表或文件產生時標記需動態填入的區塊名稱。
    // 格式為 _param=<區塊名稱>_，由樣板引擎解析後替換為實際內容。
    // 帶 "M" 後綴者表示行動版（Mobile）版面配置。
    // -------------------------------------------------------------------------

    /** 樣板標題區塊佔位符（桌面版） */
    public static final String PARAM_TITLE = "_param=title_";

    /** 樣板標題區塊佔位符（行動版） */
    public static final String PARAM_TITLEM = "_param=titlem_";

    /** 樣板內容區塊佔位符（桌面版） */
    public static final String PARAM_CONTENT = "_param=content_";

    /** 樣板內容區塊佔位符（行動版） */
    public static final String PARAM_CONTENTM = "_param=contentm_";

    /** 樣板頁首區塊佔位符（桌面版） */
    public static final String PARAM_HEADER = "_param=header_";

    /** 樣板頁首區塊佔位符（行動版） */
    public static final String PARAM_HEADERM = "_param=headerm_";

    /** 樣板主體區塊佔位符（桌面版） */
    public static final String PARAM_BODY = "_param=body_";

    /** 樣板主體區塊佔位符（行動版） */
    public static final String PARAM_BODYM = "_param=bodym_";

    /** 樣板頁尾區塊佔位符（桌面版） */
    public static final String PARAM_FOOTER = "_param=footer_";

    /** 樣板頁尾區塊佔位符（行動版） */
    public static final String PARAM_FOOTERM = "_param=footerm_";

    /** 樣板合計區塊佔位符（桌面版） */
    public static final String PARAM_TOTAL = "_param=total_";

    /** 樣板合計區塊佔位符（行動版） */
    public static final String PARAM_TOTALM = "_param=totalm_";

    /** 樣板空白區塊佔位符 */
    public static final String PARAM_BLANK = "_param=blank_";

    /** 樣板換行佔位符，指示樣板引擎在此位置插入空白列 */
    public static final String PARAM_SKIP_A_LINE = "_param=skipALine_";

    /** 樣板合併欄位佔位符，指示樣板引擎在此位置合併多欄 */
    public static final String PARAM_MERGE_COLUMNS = "_param=mergeColumns_";

    // -------------------------------------------------------------------------
    // 數字型樣板參數值（=1 ~ =20）
    // 用於在樣板中傳遞數值型參數，如欄位索引、資料列編號等。
    // -------------------------------------------------------------------------

    /** 樣板數字參數值：=1 */
    public static final String PAPAM_1 = "=1";
    /** 樣板數字參數值：=2 */
    public static final String PAPAM_2 = "=2";
    /** 樣板數字參數值：=3 */
    public static final String PAPAM_3 = "=3";
    /** 樣板數字參數值：=4 */
    public static final String PAPAM_4 = "=4";
    /** 樣板數字參數值：=5 */
    public static final String PAPAM_5 = "=5";
    /** 樣板數字參數值：=6 */
    public static final String PAPAM_6 = "=6";
    /** 樣板數字參數值：=7 */
    public static final String PAPAM_7 = "=7";
    /** 樣板數字參數值：=8 */
    public static final String PAPAM_8 = "=8";
    /** 樣板數字參數值：=9 */
    public static final String PAPAM_9 = "=9";
    /** 樣板數字參數值：=10 */
    public static final String PAPAM_10 = "=10";
    /** 樣板數字參數值：=11 */
    public static final String PAPAM_11 = "=11";
    /** 樣板數字參數值：=12 */
    public static final String PAPAM_12 = "=12";
    /** 樣板數字參數值：=13 */
    public static final String PAPAM_13 = "=13";
    /** 樣板數字參數值：=14 */
    public static final String PAPAM_14 = "=14";
    /** 樣板數字參數值：=15 */
    public static final String PAPAM_15 = "=15";
    /** 樣板數字參數值：=16 */
    public static final String PAPAM_16 = "=16";
    /** 樣板數字參數值：=17 */
    public static final String PAPAM_17 = "=17";
    /** 樣板數字參數值：=18 */
    public static final String PAPAM_18 = "=18";
    /** 樣板數字參數值：=19 */
    public static final String PAPAM_19 = "=19";
    /** 樣板數字參數值：=20 */
    public static final String PAPAM_20 = "=20";

    /** 樣板參數名稱的底線前綴識別子，用於解析時判斷是否為參數佔位符 */
    public static final String UNDERLINE_PARAM = "_param";

    public StringConstant() {
    }
}
