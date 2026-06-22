package com.zipe.util.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正規表達式驗證工具類別。
 * <p>
 * 提供常用格式的字串驗證方法，包含電子郵件、電話號碼、IP 位址、
 * URL、日期、中文字元、數字等格式的正規表達式比對。
 * 所有方法皆為靜態方法，可直接呼叫，無需建立實例。
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/1/4 下午 01:05
 **/
public class RegexUtils {
    /**
     * 驗證電子郵件地址格式。
     *
     * @param email 電子郵件地址，格式範例：user@example.com、user@example.com.tw，
     *              其中網域部分可包含二級網域（如 .com.tw）
     * @return 驗證成功回傳 {@code true}，驗證失敗回傳 {@code false}
     */
    public static boolean checkEmail(String email) {
        String regex = "\\w+@\\w+\\.[a-z]+(\\.[a-z]+)?";
        return Pattern.matches(regex, email);
    }

    /**
     * 驗證行動電話號碼（支援國際冠碼格式，例如 +86135xxxx...、0086135xxxx...）。
     * <p>支援的號碼段說明（依電信業者）：</p>
     * <p>移動的號段：134(0-8)、135、136、137、138、139、147（預計用於 TD 上網卡）、
     * 150、151、152、157（TD 專用）、158、159、187（未啟用）、188（TD 專用）</p>
     * <p>聯通的號段：130、131、132、155、156（世界風專用）、185（未啟用）、186（3G）</p>
     * <p>電信的號段：133、153、180（未啟用）、189</p>
     *
     * @param mobile 待驗證的行動電話號碼字串
     * @return 驗證成功回傳 {@code true}，驗證失敗回傳 {@code false}
     */
    public static boolean checkMobile(String mobile) {
        String regex = "(\\+\\d+)?1[34578]\\d{9}$";
        return Pattern.matches(regex,mobile);
    }
    /**
     * 驗證市話（固定電話）號碼格式。
     * <p>格式說明：國碼 + 區碼（城市代碼）+ 電話號碼，例如：+8602085588447</p>
     * <p><b>國碼：</b>代表國家或地區的標準代碼，由一到多位 0-9 數字組成，後接空格。</p>
     * <p><b>區碼（城市代碼）：</b>可包含一到多位 0-9 數字，以括號標示；
     * 不使用區碼的國家或地區可省略此部分。</p>
     * <p><b>電話號碼：</b>由一到多位 0-9 數字組成。</p>
     *
     * @param phone 待驗證的市話號碼字串
     * @return 驗證成功回傳 {@code true}，驗證失敗回傳 {@code false}
     */
    public static boolean checkPhone(String phone) {
        String regex = "(\\+\\d+)?(\\d{3,4}\\-?)?\\d{7,8}$";
        return Pattern.matches(regex, phone);
    }
    /**
     * 驗證整數（含正整數與負整數）。
     *
     * @param digit 待驗證的整數字串，由一位或多位 0-9 組成，可帶負號
     * @return 驗證成功回傳 {@code true}，驗證失敗回傳 {@code false}
     */
    public static boolean checkDigit(String digit) {
        String regex = "\\-?[1-9]\\d*";
        return Pattern.matches(regex,digit);
    }
    /**
     * 驗證整數與浮點數（含正負整數及正負浮點數）。
     *
     * @param decimals 待驗證的數字字串，可為整數或浮點數（含小數點），例如：1.23、233.30
     * @return 驗證成功回傳 {@code true}，驗證失敗回傳 {@code false}
     */
    public static boolean checkDecimals(String decimals) {
        String regex = "\\-?[1-9]\\d*(\\.\\d+)?";
        return Pattern.matches(regex,decimals);
    }
    /**
     * 驗證字串是否全為空白字元。
     * <p>空白字元包含：空格（&#32;）、\t（水平定位字元）、\n（換行）、
     * \r（歸位）、\f（換頁）、\x0B（垂直定位字元）。</p>
     *
     * @param blankSpace 待驗證的字串
     * @return 驗證成功回傳 {@code true}，驗證失敗回傳 {@code false}
     */
    public static boolean checkBlankSpace(String blankSpace) {
        String regex = "\\s+";
        return Pattern.matches(regex,blankSpace);
    }
    /**
     * 驗證字串是否全由中文字元組成。
     * <p>比對範圍為 Unicode CJK 統一表意文字區塊（U+4E00 至 U+9FA5），
     * 即常見的繁體與簡體漢字。</p>
     *
     * @param chinese 待驗證的字串
     * @return 驗證成功回傳 {@code true}，驗證失敗回傳 {@code false}
     */
    public static boolean checkChinese(String chinese) {
        String regex = "^[\u4E00-\u9FA5]+$";
        return Pattern.matches(regex,chinese);
    }
    /**
     * 驗證日期字串（年月日格式）。
     * <p>支援以 {@code -}、{@code /}、{@code .} 作為分隔符號，
     * 例如：1992-09-03、1992/09/03、1992.09.03。</p>
     *
     * @param birthday 待驗證的日期字串
     * @return 驗證成功回傳 {@code true}，驗證失敗回傳 {@code false}
     */
    public static boolean checkBirthday(String birthday) {
        // 使用反向參照 \1 確保年月日的分隔符號一致，避免混用不同符號
        String regex = "\\d{4}([-./])\\d{1,2}\\1\\d{1,2}";
        return Pattern.matches(regex,birthday);
    }
    /**
     * 驗證 URL 網址格式。
     * <p>支援 http 及 https 協定，可帶埠號與路徑，
     * 例如：http://blog.csdn.net:80/xyang81/article/details/7705960? 或 http://www.csdn.net:80。</p>
     *
     * @param url 待驗證的 URL 字串
     * @return 驗證成功回傳 {@code true}，驗證失敗回傳 {@code false}
     */
    public static boolean checkURL(String url) {
        String regex = "(https?://(w{3}\\.)?)?\\w+\\.\\w+(\\.[a-zA-Z]+)*(:\\d{1,5})?(/\\w*)*(\\??(.+=.*)?(&.+=.*)?)?";
        return Pattern.matches(regex, url);
    }
    /**
     * 取得網址 URL 的一級網域（頂層域名）。
     * <p>例如傳入 {@code http://blog.example.com.cn/path}，
     * 回傳 {@code example.com.cn}。</p>
     * <p>支援的頂層域名後綴：com、cn、net、org、biz、info、cc、tv。</p>
     *
     * @param url 完整的 URL 字串，需包含 {@code http://} 協定頭
     * @return 解析出的一級網域字串
     */
    public static String getDomain(String url) {
        Pattern p = Pattern.compile("(?<=http://|\\.)[^.]*?\\.(com|cn|net|org|biz|info|cc|tv)", Pattern.CASE_INSENSITIVE);
// 獲取完整的域名
// Pattern p=Pattern.compile("[^//]*?\\.(com|cn|net|org|biz|info|cc|tv)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = p.matcher(url);
        matcher.find();
        return matcher.group();
    }
    /**
     * 驗證 IPv4 位址格式（簡易比對，僅驗證格式，不驗證各段數值範圍）。
     * <p>格式範例：192.168.1.1、127.0.0.1。
     * 注意：此方法不驗證各段數值是否落在 0–255 的合法範圍內。</p>
     *
     * @param ipAddress 待驗證的 IPv4 位址字串
     * @return 驗證成功回傳 {@code true}，驗證失敗回傳 {@code false}
     */
    public static boolean checkIpAddress(String ipAddress) {
        String regex = "[1-9](\\d{1,2})?\\.(0|([1-9](\\d{1,2})?))\\.(0|([1-9](\\d{1,2})?))\\.(0|([1-9](\\d{1,2})?))";
        return Pattern.matches(regex, ipAddress);
    }

}
