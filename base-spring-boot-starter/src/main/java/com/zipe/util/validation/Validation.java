package com.zipe.util.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
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
    public static final Pattern TWPID_PATTERN = Pattern
            .compile("[ABCDEFGHJKLMNPQRSTUVXYWZIO][12]\\d{8}");
////------------------驗證方法

    /**
     * 判斷欄位是否為空 符合返回ture
     *
     * @param str
     * @return boolean
     */
    public static synchronized boolean StrisNull(String str) {
        return null == str || str.trim().length() <= 0 ? true : false;
    }

    /**
     * 判斷欄位是非空 符合返回ture
     *
     * @param str
     * @return boolean
     */
    public static boolean StrNotNull(String str) {
        return !StrisNull(str);
    }

    /**
     * 字串null轉空
     *
     * @param str
     * @return boolean
     */
    public static String nulltoStr(String str) {
        return StrisNull(str) ? "" : str;
    }

    /**
     * 字串null賦值預設值
     *
     * @param str    目標字串
     * @param defaut 預設值
     * @return String
     */
    public static String nulltoStr(String str, String defaut) {
        return StrisNull(str) ? defaut : str;
    }

    /**
     * 判斷欄位是否為Email 符合返回ture
     *
     * @param str
     * @return boolean
     */
    public static boolean isEmail(String str) {
        return Regular(str, EMAIL);
    }

    /**
     * 判斷是否為電話號碼 符合返回ture
     *
     * @param str
     * @return boolean
     */
    public static boolean isPhone(String str) {
        return Regular(str, PHONE);
    }

    /**
     * 判斷是否為手機號碼 符合返回ture
     *
     * @param str
     * @return boolean
     */
    public static boolean isMobile(String str) {
        return Regular(str, MOBILE);
    }

    /**
     * 判斷是否為Url 符合返回ture
     *
     * @param str
     * @return boolean
     */
    public static boolean isUrl(String str) {
        return Regular(str, URL);
    }

    /**
     * 判斷欄位是否為數字 正負整數 正負浮點數 符合返回ture
     *
     * @param str
     * @return boolean
     */
    public static boolean isNumber(String str) {
        return Regular(str, DOUBLE);
    }

    /**
     * 判斷欄位是否為INTEGER 符合返回ture
     *
     * @param str
     * @return boolean
     */
    public static boolean isInteger(String str) {
        return Regular(str, INTEGER);
    }

    /**
     * 判斷欄位是否為正整數正規表示式 >=0 符合返回ture
     *
     * @param str
     * @return boolean
     */
    public static boolean isINTEGER_NEGATIVE(String str) {
        return Regular(str, INTEGER_NEGATIVE);
    }

    /**
     * 判斷欄位是否為負整數正規表示式 <=0 符合返回ture
     *
     * @param str
     * @return boolean
     */
    public static boolean isINTEGER_POSITIVE(String str) {
        return Regular(str, INTEGER_POSITIVE);
    }

    /**
     * 判斷欄位是否為DOUBLE 符合返回ture
     *
     * @param str
     * @return boolean
     */
    public static boolean isDouble(String str) {
        return Regular(str, DOUBLE);
    }

    /**
     * 判斷欄位是否為正浮點數正規表示式 >=0 符合返回ture
     *
     * @param str
     * @return boolean
     */
    public static boolean isDOUBLE_NEGATIVE(String str) {
        return Regular(str, DOUBLE_NEGATIVE);
    }

    /**
     * 判斷欄位是否為負浮點數正規表示式 <=0 符合返回ture
     *
     * @param str
     * @return boolean
     */
    public static boolean isDOUBLE_POSITIVE(String str) {
        return Regular(str, DOUBLE_POSITIVE);
    }

    /**
     * 判斷欄位是否為日期 符合返回ture
     *
     * @param str
     * @return boolean
     */
    public static boolean isDate(String str) {
        return Regular(str, DATE_ALL);
    }

    /**
     * 驗證2010-12-10
     *
     * @param str
     * @return
     */
    public static boolean isDate1(String str) {
        return Regular(str, DATE_FORMAT1);
    }

    /**
     * 判斷欄位是否為年齡 符合返回ture
     *
     * @param str
     * @return boolean
     */
    public static boolean isAge(String str) {
        return Regular(str, AGE);
    }

    /**
     * 判斷欄位是否超長
     * 字串為空返回fasle, 超過長度{leng}返回ture 反之返回false
     *
     * @param str
     * @param leng
     * @return boolean
     */
    public static boolean isLengOut(String str, int leng) {
        return StrisNull(str) ? false : str.trim().length() > leng;
    }

    /**
     * 判斷欄位是否為身份證 符合返回ture
     *
     * @param str
     * @return boolean
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
     * @since 2006/07/19
     */
    public static boolean isValidTWPID(String twpid) {
        boolean result = false;
        String pattern = "ABCDEFGHJKLMNPQRSTUVXYWZIO";
        if (TWPID_PATTERN.matcher(twpid.toUpperCase()).matches()) {
            int code = pattern.indexOf(twpid.toUpperCase().charAt(0)) + 10;
            int sum = 0;
            sum = (int) (code / 10) + 9 * (code % 10) + 8 * (twpid.charAt(1) - '0')
                    + 7 * (twpid.charAt(2) - '0') + 6 * (twpid.charAt(3) - '0')
                    + 5 * (twpid.charAt(4) - '0') + 4 * (twpid.charAt(5) - '0')
                    + 3 * (twpid.charAt(6) - '0') + 2 * (twpid.charAt(7) - '0')
                    + 1 * (twpid.charAt(8) - '0') + (twpid.charAt(9) - '0');
            if ( (sum % 10) == 0) {
                result = true;
            }
        }
        return result;
    }
    /**
     * 判斷欄位是否為郵編 符合返回ture
     *
     * @param str
     * @return boolean
     */
    public static boolean isCode(String str) {
        return Regular(str, CODE);
    }

    /**
     * 判斷字串是不是全部是英文字母
     *
     * @param str
     * @return boolean
     */
    public static boolean isEnglish(String str) {
        return Regular(str, STR_ENG);
    }

    /**
     * 判斷字串是不是全部是英文字母 數字
     *
     * @param str
     * @return boolean
     */
    public static boolean isENG_NUM(String str) {
        return Regular(str, STR_ENG_NUM);
    }

    /**
     * 判斷字串是不是全部是英文字母 數字 下劃線
     *
     * @param str
     * @return boolean
     */
    public static boolean isENG_NUM_(String str) {
        return Regular(str, STR_ENG_NUM_);
    }

    /**
     * 過濾特殊字串 返回過濾後的字串
     *
     * @param str
     * @return boolean
     */
    public static String filterStr(String str) {
        Pattern p = Pattern.compile(STR_SPECIAL);
        Matcher m = p.matcher(str);
        return m.replaceAll("").trim();
    }

    /**
     * 校驗機構程式碼格式
     *
     * @return
     */
    public static boolean isJigouCode(String str) {
        return Regular(str, JIGOU_CODE);
    }

    /**
     * 判斷字串是不是數字組成
     *
     * @param str
     * @return boolean
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
