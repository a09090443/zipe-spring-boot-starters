package com.zipe.util.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 資料驗證工具類別，提供常用格式的正規表達式常數與對應的靜態驗證方法。
 * <p>
 * 支援的驗證類型包含：Email、電話號碼、手機號碼、URL、數字（整數／浮點數）、
 * 日期、年齡、郵遞區號、英文字母、身份證（中國大陸格式）以及台灣身分證字號（ROC PID）。
 * </p>
 * <p>
 * 所有方法均為靜態方法，無需實例化即可直接使用。
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/1/4 下午 01:07
 **/
public class Validation {
//------------------常量定義
    /**
     * Email正規表示式="^([a-z0-9A-Z] [-|\\.]?) [a-z0-9A-Z]@([a-z0-9A-Z] (-[a-z0-9A-Z] )?\\.) [a-zA-Z]{2,}$";
     */
//public static final String EMAIL = "^([a-z0-9A-Z] [-|\\.]?) [a-z0-9A-Z]@([a-z0-9A-Z] (-[a-z0-9A-Z] )?\\.) [a-zA-Z]{2,}$";;
    public static final String EMAIL = "\\w (\\.\\w )*@\\w (\\.\\w ) ";
    /**
     * 電話號碼正規表示式= (^(\d{2,4}[-_－—]?)?\d{3,8}([-_－—]?\d{3,8})?([-_－—]?\d{1,7})?$)|(^0?1[35]\d{9}$)
     */
    public static final String PHONE = "(^(\\d{2,4}[-_－—]?)?\\d{3,8}([-_－—]?\\d{3,8})?([-_－—]?\\d{1,7})?$)|(^0?1[35]\\d{9}$)";
    /**
     * 手機號碼正規表示式=^(13[0-9]|14[0-9]|15[0-9]|17[0-9]|18[0-9])\d{8}$
     */
    public static final String MOBILE = "^(13[0-9]|14[0-9]|15[0-9]|17[0-9]|18[0-9])\\d{8}$";
    /**
     * Integer正規表示式 ^-?(([1-9]\d*$)|0)
     */
    public static final String INTEGER = "^-?(([1-9]\\d*$)|0)";
    /**
     * 正整數正規表示式 >=0 ^[1-9]\d*|0$
     */
    public static final String INTEGER_NEGATIVE = "^[1-9]\\d*|0$";
    /**
     * 負整數正規表示式 <=0 ^-[1-9]\d*|0$
     */
    public static final String INTEGER_POSITIVE = "^-[1-9]\\d*|0$";
    /**
     * Double正規表示式 ^-?([1-9]\d*\.\d*|0\.\d*[1-9]\d*|0?\.0 |0)$
     */
    public static final String DOUBLE = "^-?([1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|0?\\.0 |0)$";
    /**
     * 正Double正規表示式 >=0 ^[1-9]\d*\.\d*|0\.\d*[1-9]\d*|0?\.0 |0$
     */
    public static final String DOUBLE_NEGATIVE = "^[1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|0?\\.0 |0$";
    /**
     * 負Double正規表示式 <= 0 ^(-([1-9]\d*\.\d*|0\.\d*[1-9]\d*))|0?\.0 |0$
     */
    public static final String DOUBLE_POSITIVE = "^(-([1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*))|0?\\.0 |0$";
    /**
     * 年齡正規表示式 ^(?:[1-9][0-9]?|1[01][0-9]|120)$ 匹配0-120歲
     */
    public static final String AGE = "^(?:[1-9][0-9]?|1[01][0-9]|120)$";
    /**
     * 郵編正規表示式 [0-9]\d{5}(?!\d) 國內6位郵編
     */
    public static final String CODE = "[0-9]\\d{5}(?!\\d)";
    /**
     * 匹配由數字、26個英文字母或者下劃線組成的字串 ^\w $
     */
    public static final String STR_ENG_NUM_ = "^\\w $";
    /**
     * 匹配由數字和26個英文字母組成的字串 ^[A-Za-z0-9] $
     */
    public static final String STR_ENG_NUM = "^[A-Za-z0-9] ";
    /**
     * 匹配由26個英文字母組成的字串 ^[A-Za-z] $
     */
    public static final String STR_ENG = "^[A-Za-z] $";
    /**
     * 過濾特殊字串正則
     * regEx="[`~!@#$%^&*() =|{}':;',\\[\\].<>/?~！@#￥%……&*（）—— |{}【】‘；：”“'。，、？]";
     */
    public static final String STR_SPECIAL = "[`~!@#$%^&*() =|{}':;',\\[\\].<>/?~！@#￥%……&*（）—— |{}【】‘；：”“'。，、？]";
    /***
     * 日期正則 支援：
     * YYYY-MM-DD
     * YYYY/MM/DD
     * YYYY_MM_DD
     * YYYYMMDD
     * YYYY.MM.DD的形式
     */
    public static final String DATE_ALL = "((^((1[8-9]\\d{2})|([2-9]\\d{3}))([-\\/\\._]?)(10|12|0?[13578])([-\\/\\._]?)(3[01]|[12][0-9]|0?[1-9])$)" +
            "|(^((1[8-9]\\d{2})|([2-9]\\d{3}))([-\\/\\._]?)(11|0?[469])([-\\/\\._]?)(30|[12][0-9]|0?[1-9])$)" +
            "|(^((1[8-9]\\d{2})|([2-9]\\d{3}))([-\\/\\._]?)(0?2)([-\\/\\._]?)(2[0-8]|1[0-9]|0?[1-9])$)|(^([2468][048]00)([-\\/\\._]?)(0?2)([-\\/\\._]?)(29)$)|(^([3579][26]00)" +
            "([-\\/\\._]?)(0?2)([-\\/\\._]?)(29)$)" +
            "|(^([1][89][0][48])([-\\/\\._]?)(0?2)([-\\/\\._]?)(29)$)|(^([2-9][0-9][0][48])([-\\/\\._]?)" +
            "(0?2)([-\\/\\._]?)(29)$)" +
            "|(^([1][89][2468][048])([-\\/\\._]?)(0?2)([-\\/\\._]?)(29)$)|(^([2-9][0-9][2468][048])([-\\/\\._]?)(0?2)" +
            "([-\\/\\._]?)(29)$)|(^([1][89][13579][26])([-\\/\\._]?)(0?2)([-\\/\\._]?)(29)$)|" +
            "(^([2-9][0-9][13579][26])([-\\/\\._]?)(0?2)([-\\/\\._]?)(29)$))";
    /***
     * 日期正則 支援：
     * YYYY-MM-DD
     */
    public static final String DATE_FORMAT1 = "(([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})-(((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)-(0[1-9]|[12][0-9]|30))|(02-(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))-02-29)";
    /**
     * URL正規表示式
     * 匹配 http www ftp
     */
    public static final String URL = "^(http|www|ftp|)?(://)?(\\w (-\\w )*)(\\.(\\w (-\\w )*))*((:\\d )?)(/(\\w (-\\w )*))*(\\.?(\\w)*)(\\?)?" +
            "(((\\w*%)*(\\w*\\?)*(\\w*:)*(\\w*\\ )*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*(\\w*%)*(\\w*\\?)*" +
            "(\\w*:)*(\\w*\\ )*(\\w*\\.)*" +
            "(\\w*&)*(\\w*-)*(\\w*=)*)*(\\w*)*)$";
    /**
     * 身份證正規表示式
     */
    public static final String IDCARD = "((11|12|13|14|15|21|22|23|31|32|33|34|35|36|37|41|42|43|44|45|46|50|51|52|53|54|61|62|63|64|65)[0-9]{4})" +
            "(([1|2][0-9]{3}[0|1][0-9][0-3][0-9][0-9]{3}" +
            "[Xx0-9])|([0-9]{2}[0|1][0-9][0-3][0-9][0-9]{3}))";
    /**
     * 機構程式碼
     */
    public static final String JIGOU_CODE = "^[A-Z0-9]{8}-[A-Z0-9]$";
    /**
     * 匹配數字組成的字串 [0-9]*
     */
    public static final String STR_NUM = "[0-9]*";
    /**
     * 台灣身分證字號預先編譯的正規表達式 Pattern。
     * 第一碼為縣市代碼字母（排除 I），第二碼為性別碼（1=男，2=女），後接 8 位數字。
     * 預先編譯可避免每次呼叫 {@link #isValidTWPID(String)} 時重複編譯，提升效能。
     */
    public static final Pattern TWPID_PATTERN = Pattern
            .compile("[ABCDEFGHJKLMNPQRSTUVXYWZIO][12]\\d{8}");
////------------------驗證方法

    /**
     * 判斷字串是否為空值（null 或空白字串）。
     *
     * @param str 待檢查的字串
     * @return 若字串為 {@code null} 或去除前後空白後長度為 0，則回傳 {@code true}；否則回傳 {@code false}
     */
    public static synchronized boolean StrisNull(String str) {
        return null == str || str.trim().length() <= 0 ? true : false;
    }

    /**
     * 判斷字串是否為非空值。
     *
     * @param str 待檢查的字串
     * @return 若字串不為 {@code null} 且去除前後空白後長度大於 0，則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean StrNotNull(String str) {
        return !StrisNull(str);
    }

    /**
     * 將 {@code null} 字串轉換為空字串，非 {@code null} 則原樣回傳。
     *
     * @param str 待轉換的字串，允許為 {@code null}
     * @return 若 {@code str} 為空值則回傳 {@code ""}；否則回傳原字串
     */
    public static String nulltoStr(String str) {
        return StrisNull(str) ? "" : str;
    }

    /**
     * 將 {@code null} 字串替換為指定的預設值，非 {@code null} 則原樣回傳。
     *
     * @param str    目標字串，允許為 {@code null}
     * @param defaut 當 {@code str} 為空值時要回傳的預設值
     * @return 若 {@code str} 為空值則回傳 {@code defaut}；否則回傳原字串
     */
    public static String nulltoStr(String str, String defaut) {
        return StrisNull(str) ? defaut : str;
    }

    /**
     * 判斷字串是否符合 Email 格式。
     *
     * @param str 待驗證的字串
     * @return 符合 Email 格式則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isEmail(String str) {
        return Regular(str, EMAIL);
    }

    /**
     * 判斷字串是否符合電話號碼格式（含區碼）。
     *
     * @param str 待驗證的字串
     * @return 符合電話號碼格式則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isPhone(String str) {
        return Regular(str, PHONE);
    }

    /**
     * 判斷字串是否符合手機號碼格式（13x / 14x / 15x / 17x / 18x 開頭，共 11 碼）。
     *
     * @param str 待驗證的字串
     * @return 符合手機號碼格式則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isMobile(String str) {
        return Regular(str, MOBILE);
    }

    /**
     * 判斷字串是否符合 URL 格式（支援 http、www、ftp 等協定）。
     *
     * @param str 待驗證的字串
     * @return 符合 URL 格式則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isUrl(String str) {
        return Regular(str, URL);
    }

    /**
     * 判斷欄位是否為數字（正負整數或正負浮點數）。
     *
     * @param str 待驗證的字串
     * @return 符合數字格式則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isNumber(String str) {
        return Regular(str, DOUBLE);
    }

    /**
     * 判斷欄位是否為整數（含正整數、負整數與零）。
     *
     * @param str 待驗證的字串
     * @return 符合整數格式則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isInteger(String str) {
        return Regular(str, INTEGER);
    }

    /**
     * 判斷欄位是否為非負整數（&gt;= 0）。
     *
     * @param str 待驗證的字串
     * @return 符合非負整數格式則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isINTEGER_NEGATIVE(String str) {
        return Regular(str, INTEGER_NEGATIVE);
    }

    /**
     * 判斷欄位是否為非正整數（&lt;= 0）。
     *
     * @param str 待驗證的字串
     * @return 符合非正整數格式則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isINTEGER_POSITIVE(String str) {
        return Regular(str, INTEGER_POSITIVE);
    }

    /**
     * 判斷欄位是否為浮點數（含正負浮點數與零）。
     *
     * @param str 待驗證的字串
     * @return 符合浮點數格式則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isDouble(String str) {
        return Regular(str, DOUBLE);
    }

    /**
     * 判斷欄位是否為非負浮點數（&gt;= 0）。
     *
     * @param str 待驗證的字串
     * @return 符合非負浮點數格式則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isDOUBLE_NEGATIVE(String str) {
        return Regular(str, DOUBLE_NEGATIVE);
    }

    /**
     * 判斷欄位是否為非正浮點數（&lt;= 0）。
     *
     * @param str 待驗證的字串
     * @return 符合非正浮點數格式則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isDOUBLE_POSITIVE(String str) {
        return Regular(str, DOUBLE_POSITIVE);
    }

    /**
     * 判斷欄位是否為有效日期（支援多種分隔符格式，涵蓋西元 1800 年以後的合法日期，含閏年判斷）。
     *
     * @param str 待驗證的字串
     * @return 符合日期格式則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isDate(String str) {
        return Regular(str, DATE_ALL);
    }

    /**
     * 判斷欄位是否符合 YYYY-MM-DD 格式的嚴格日期（含閏年二月 29 日驗證）。
     *
     * @param str 待驗證的字串，格式須為 YYYY-MM-DD（例如 2010-12-10）
     * @return 符合 YYYY-MM-DD 日期格式則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isDate1(String str) {
        return Regular(str, DATE_FORMAT1);
    }

    /**
     * 判斷欄位是否為合法年齡（0 至 120 歲之間的整數）。
     *
     * @param str 待驗證的字串
     * @return 符合年齡範圍則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isAge(String str) {
        return Regular(str, AGE);
    }

    /**
     * 判斷字串長度是否超過指定上限。
     * 字串為 {@code null} 或空白時視為未超長，直接回傳 {@code false}。
     *
     * @param str  待檢查的字串，允許為 {@code null}
     * @param leng 長度上限（不含）
     * @return 字串去除前後空白後長度超過 {@code leng} 則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isLengOut(String str, int leng) {
        return StrisNull(str) ? false : str.trim().length() > leng;
    }

    /**
     * 判斷欄位是否符合中國大陸身份證號碼格式（15 碼舊版或 18 碼新版）。
     *
     * @param str 待驗證的字串
     * @return 符合身份證號碼格式則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isIdCard(String str) {
        if (StrisNull(str)) return false;
        if (str.trim().length() == 15 || str.trim().length() == 18) {
            return Regular(str, IDCARD);
        } else {
            return false;
        }
    }
    /**
     * 身分證字號檢查程式，身分證字號規則：
     * 字母(ABCDEFGHJKLMNPQRSTUVXYWZIO)對應一組數(10~35)，
     * 令其十位數為X1，個位數為X2；( 如Ａ：X1=1 , X2=0 )；D表示2~9數字
     * Y = X1 + 9*X2 + 8*D1 + 7*D2 + 6*D3 + 5*D4 + 4*D5 + 3*D6 + 2*D7+ 1*D8 + D9
     * 如Y能被10整除，則表示該身分證號碼為正確，否則為錯誤。
     * 臺北市(A)、臺中市(B)、基隆市(C)、臺南市(D)、高雄市(E)、臺北縣(F)、
     * 宜蘭縣(G)、桃園縣(H)、嘉義市(I)、新竹縣(J)、苗栗縣(K)、臺中縣(L)、
     * 南投縣(M)、彰化縣(N)、新竹市(O)、雲林縣(P)、嘉義縣(Q)、臺南縣(R)、
     * 高雄縣(S)、屏東縣(T)、花蓮縣(U)、臺東縣(V)、金門縣(W)、澎湖縣(X)、
     * 陽明山(Y)、連江縣(Z)
     *
     * @param twpid 待驗證的台灣身分證字號字串（不區分大小寫）
     * @return 身分證字號格式與校驗碼皆正確則回傳 {@code true}；否則回傳 {@code false}
     * @since 2006/07/19
     */
    public static boolean isValidTWPID(String twpid) {
        boolean result = false;
        // 縣市代碼字母對應表，索引值加 10 即為兩位數代碼（例如 A → 索引 0 → 代碼 10）
        String pattern = "ABCDEFGHJKLMNPQRSTUVXYWZIO";
        if (TWPID_PATTERN.matcher(twpid.toUpperCase()).matches()) {
            // 取得首碼字母對應的數值（10~35）
            int code = pattern.indexOf(twpid.toUpperCase().charAt(0)) + 10;
            int sum = 0;
            // 依加權公式計算檢查碼：X1 + 9*X2 + 8*D1 + ... + 1*D8 + D9
            // 其中 X1 為代碼十位數，X2 為代碼個位數，D1~D9 為後 9 碼數字
            sum = (int) (code / 10) + 9 * (code % 10) + 8 * (twpid.charAt(1) - '0')
                    + 7 * (twpid.charAt(2) - '0') + 6 * (twpid.charAt(3) - '0')
                    + 5 * (twpid.charAt(4) - '0') + 4 * (twpid.charAt(5) - '0')
                    + 3 * (twpid.charAt(6) - '0') + 2 * (twpid.charAt(7) - '0')
                    + 1 * (twpid.charAt(8) - '0') + (twpid.charAt(9) - '0');
            // 總和可被 10 整除表示校驗碼正確
            if ( (sum % 10) == 0) {
                result = true;
            }
        }
        return result;
    }
    /**
     * 判斷欄位是否為 6 位數郵遞區號格式。
     *
     * @param str 待驗證的字串
     * @return 符合郵遞區號格式則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isCode(String str) {
        return Regular(str, CODE);
    }

    /**
     * 判斷字串是否全部由英文字母（A-Z、a-z）組成。
     *
     * @param str 待驗證的字串
     * @return 全為英文字母則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isEnglish(String str) {
        return Regular(str, STR_ENG);
    }

    /**
     * 判斷字串是否全部由英文字母（A-Z、a-z）與數字（0-9）組成。
     *
     * @param str 待驗證的字串
     * @return 全為英文字母與數字則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isENG_NUM(String str) {
        return Regular(str, STR_ENG_NUM);
    }

    /**
     * 判斷字串是否全部由英文字母、數字與底線（_）組成。
     *
     * @param str 待驗證的字串
     * @return 全為英文字母、數字或底線則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isENG_NUM_(String str) {
        return Regular(str, STR_ENG_NUM_);
    }

    /**
     * 過濾字串中的特殊符號，回傳去除特殊字元後的結果。
     *
     * @param str 待過濾的字串
     * @return 去除所有特殊字元並去除前後空白後的字串
     */
    public static String filterStr(String str) {
        Pattern p = Pattern.compile(STR_SPECIAL);
        Matcher m = p.matcher(str);
        return m.replaceAll("").trim();
    }

    /**
     * 校驗機構程式碼格式（格式：8 碼大寫英數字 + 連字號 + 1 碼大寫英數字）。
     *
     * @param str 待驗證的字串
     * @return 符合機構程式碼格式則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isJigouCode(String str) {
        return Regular(str, JIGOU_CODE);
    }

    /**
     * 判斷字串是否全部由數字（0-9）組成。
     *
     * @param str 待驗證的字串
     * @return 全為數字則回傳 {@code true}；否則回傳 {@code false}
     */
    public static boolean isSTR_NUM(String str) {
        return Regular(str, STR_NUM);
    }

    /**
     * 匹配是否符合正規表示式pattern 匹配返回true
     *
     * @param str     匹配的字串
     * @param pattern 匹配模式
     * @return boolean
     */
    private static boolean Regular(String str, String pattern) {
        if (null == str || str.trim().length() <= 0)
            return false;
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(str);
        return m.matches();
    }
}
