package com.zipe.util.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/1/4 下午 01:05
 **/
public class RegexUtils {
    /**
     * 驗證Email
     * @param email email地址，格式：zhangsan@zuidaima.com，zhangsan@xxx.com.cn，xxx代表郵件服務商
     * @return 驗證成功返回true，驗證失敗返回false
     */
    public static boolean checkEmail(String email) {
        String regex = "\\w @\\w \\.[a-z] (\\.[a-z] )?";
        return Pattern.matches(regex, email);
    }

    /**
     * 驗證手機號碼（支援國際格式， 86135xxxx...（中國內地）， 00852137xxxx...（中國香港））
     * @param mobile 移動、聯通、電信運營商的號碼段
     *<p>移動的號段：134(0-8)、135、136、137、138、139、147（預計用於TD上網絡卡）
     *、150、151、152、157（TD專用）、158、159、187（未啟用）、188（TD專用）</p>
     *<p>聯通的號段：130、131、132、155、156（世界風專用）、185（未啟用）、186（3g）</p>
     *<p>電信的號段：133、153、180（未啟用）、189</p>
     * @return 驗證成功返回true，驗證失敗返回false
     */
    public static boolean checkMobile(String mobile) {
        String regex = "(\\ \\d )?1[34578]\\d{9}$";
        return Pattern.matches(regex,mobile);
    }
    /**
     * 驗證固定電話號碼
     * @param phone 電話號碼，格式：國家（地區）電話程式碼   區號（城市程式碼）   電話號碼，如： 8602085588447
     * <p><b>國家（地區） 程式碼 ：</b>標識電話號碼的國家（地區）的標準國家（地區）程式碼。它包含從 0 到 9 的一位或多位數字，
     * 數字之後是空格分隔的國家（地區）程式碼。</p>
     * <p><b>區號（城市程式碼）：</b>這可能包含一個或多個從 0 到 9 的數字，地區或城市程式碼放在圓括號——
     * 對不使用地區或城市程式碼的國家（地區），則省略該元件。</p>
     * <p><b>電話號碼：</b>這包含從 0 到 9 的一個或多個數字 </p>
     * @return 驗證成功返回true，驗證失敗返回false
     */
    public static boolean checkPhone(String phone) {
        String regex = "(\\ \\d )?(\\d{3,4}\\-?)?\\d{7,8}$";
        return Pattern.matches(regex, phone);
    }
    /**
     * 驗證整數（正整數和負整數）
     * @param digit 一位或多位0-9之間的整數
     * @return 驗證成功返回true，驗證失敗返回false
     */
    public static boolean checkDigit(String digit) {
        String regex = "\\-?[1-9]\\d ";
        return Pattern.matches(regex,digit);
    }
    /**
     * 驗證整數和浮點數（正負整數和正負浮點數）
     * @param decimals 一位或多位0-9之間的浮點數，如：1.23，233.30
     * @return 驗證成功返回true，驗證失敗返回false
     */
    public static boolean checkDecimals(String decimals) {
        String regex = "\\-?[1-9]\\d (\\.\\d )?";
        return Pattern.matches(regex,decimals);
    }
    /**
     * 驗證空白字元
     * @param blankSpace 空白字元，包括：空格、\t、\n、\r、\f、\x0B
     * @return 驗證成功返回true，驗證失敗返回false
     */
    public static boolean checkBlankSpace(String blankSpace) {
        String regex = "\\s ";
        return Pattern.matches(regex,blankSpace);
    }
    /**
     * 驗證中文
     * @param chinese 中文字元
     * @return 驗證成功返回true，驗證失敗返回false
     */
    public static boolean checkChinese(String chinese) {
        String regex = "^[\u4E00-\u9FA5] $";
        return Pattern.matches(regex,chinese);
    }
    /**
     * 驗證日期（年月日）
     * @param birthday 日期，格式：1992-09-03，或1992.09.03
     * @return 驗證成功返回true，驗證失敗返回false
     */
    public static boolean checkBirthday(String birthday) {
        String regex = "[1-9]{4}([-./])\\d{1,2}\\1\\d{1,2}";
        return Pattern.matches(regex,birthday);
    }
    /**
     * 驗證URL地址
     * @param url 格式：http://blog.csdn.net:80/xyang81/article/details/7705960? 或 http://www.csdn.net:80
     * @return 驗證成功返回true，驗證失敗返回false
     */
    public static boolean checkURL(String url) {
        String regex = "(https?://(w{3}\\.)?)?\\w \\.\\w (\\.[a-zA-Z] )*(:\\d{1,5})?(/\\w*)*(\\??(. =.*)?(&. =.*)?)?";
        return Pattern.matches(regex, url);
    }
    /**
     * <pre>
     * 獲取網址 URL 的一級域
     * </pre>
     *
     * @param url
     * @return
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
     * 匹配IP地址(簡單匹配，格式，如：192.168.1.1，127.0.0.1，沒有匹配IP段的大小)
     * @param ipAddress IPv4標準地址
     * @return 驗證成功返回true，驗證失敗返回false
     */
    public static boolean checkIpAddress(String ipAddress) {
        String regex = "[1-9](\\d{1,2})?\\.(0|([1-9](\\d{1,2})?))\\.(0|([1-9](\\d{1,2})?))\\.(0|([1-9](\\d{1,2})?))";
        return Pattern.matches(regex, ipAddress);
    }

}
