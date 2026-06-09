package com.zipe.util.string;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * 通用字串工具類別，提供數字格式化、字串補位、子字串擷取、
 * 字元驗證等常用靜態工具方法。
 * <p>
 * 所有方法均為靜態（static），無需實例化即可直接呼叫。
 * 部分方法在計算長度時以 byte 為單位，因此能正確處理含有中文字的字串。
 * </p>
 */
public class CommonStringUtil {

    /** 數字格式：無千分位、無小數（例如：1234567） */
    public static String NUMBER_FORMAT0 = "#";
    /** 數字格式：有千分位、無小數（例如：1,234,567） */
    public static String NUMBER_FORMAT1 = "###,###";
    /** 數字格式：有千分位、固定兩位小數（例如：1,234,567.00） */
    public static String NUMBER_FORMAT2 = "###,###.00";
    /** 數字格式：有千分位、固定兩位小數，整數部分至少保留一位（例如：0.00） */
    public static String NUMBER_FORMAT3 = "###,##0.00";

    /** 空白字元的 byte 值，用於 byte 陣列層級的空白填充 */
    public static final byte _STRING_BYTE_BLANK = " ".getBytes()[0];
    /** 空字串常數，用於統一空值回傳或補位的預設值 */
    public static final String _STRING_BLANK = "";

    /**
     * 將數字字串依指定格式轉為格式化後的字串（例如：「###,###.00」）。
     * <p>
     * 若傳入值為空白或字串 "null"，則以 "0" 代入進行格式化。
     * </p>
     *
     * @param number       要格式化的數字字串
     * @param numberFormat 格式化樣式，可使用本類別定義的 NUMBER_FORMAT 常數
     * @return 格式化後的字串
     */
    public static String numberFormat(String number, String numberFormat) {
        DecimalFormat decFormat = new DecimalFormat(numberFormat);
        // 防止空值或字串 "null" 傳入時拋出 NumberFormatException
        if (StringUtils.isBlank(number) || number.equals("null")) {
            number = "0";
        }
        String resultStr = decFormat.format(new BigDecimal(number));
        return resultStr;
    }

    /**
     * 將整數值以左補 "0" 的方式補足到指定總長度。
     * <p>
     * 若原始數字位數已達到或超過 {@code len}，則直接回傳原始字串，不截斷。
     * </p>
     *
     * @param num 要補零的整數值（Integer 包裝型別）
     * @param len 目標總長度（位數）
     * @return 補零後的字串
     */
    public static String addZero(Integer num, int len) {
        String newStr = "";
        String str = String.valueOf(num.intValue());
        int strLength = str.length();
        // 計算需要在前方補幾個 "0"
        int addZeroLen = len - strLength;
        StringBuffer addZeroStr = new StringBuffer("");
        if (addZeroLen > 0) {
            for (int i = 0; i < addZeroLen; i++) {
                addZeroStr.append("0");
            }
            if (addZeroStr.equals("")) {
                newStr = str;
            } else {
                newStr = addZeroStr.toString() + str;
            }
        } else {
            newStr = str;
        }
        return newStr;
    }

    /**
     * 將整數值以左補 "0" 的方式補足到指定總長度（基本型別 int 多載版本）。
     * <p>
     * 若原始數字位數已達到或超過 {@code len}，則直接回傳原始字串，不截斷。
     * </p>
     *
     * @param num 要補零的整數值（基本型別 int）
     * @param len 目標總長度（位數）
     * @return 補零後的字串
     */
    public static String addZero(int num, int len) {
        String newStr = "";
        String str = String.valueOf(num);
        int strLength = str.length();
        // 計算需要在前方補幾個 "0"
        int addZeroLen = len - strLength;
        StringBuffer addZeroStr = new StringBuffer("");
        if (addZeroLen > 0) {
            for (int i = 0; i < addZeroLen; i++) {
                addZeroStr.append("0");
            }
            if (addZeroStr.equals("")) {
                newStr = str;
            } else {
                newStr = addZeroStr.toString() + str;
            }
        } else {
            newStr = str;
        }
        return newStr;
    }


    /**
     * 取得字串的 byte 長度（中文字佔 2 byte，英文字母佔 1 byte）。
     * <p>
     * 與 {@code String.length()} 不同，此方法使用 {@code getBytes()} 計算，
     * 因此能反映中文字在系統預設編碼（通常為 UTF-8 或 MS950）下的實際佔用長度。
     * </p>
     *
     * @param str 要計算長度的字串
     * @return 字串的 byte 長度
     */
    public static int getStringLength(String str) {
        return str.getBytes().length;
    }


    /**
     * 檢查字串是否為合法的數字（允許小數點，不允許負號或其他字元）。
     *
     * @param sText 要驗證的字串
     * @return 若字串只包含 0-9 與小數點則回傳 {@code true}，否則回傳 {@code false}
     */
    public static boolean isNumber(String sText) {
        // 合法字元集：數字與小數點（不含負號，因此負數將回傳 false）
        String ValidChars = "0123456789.";
        boolean IsNumber = true;
        char sChar;

        for (int i = 0; i < sText.length() && IsNumber == true; i++) {
            sChar = sText.charAt(i);
            if (ValidChars.indexOf(sChar) == -1) {
                IsNumber = false;
            }
        }
        return IsNumber;
    }

    /**
     * 檢查字串是否為合法的非負整數（不允許小數點、負號或其他字元）。
     *
     * @param sText 要驗證的字串
     * @return 若字串只包含 0-9 則回傳 {@code true}，否則回傳 {@code false}
     */
    public static boolean isInteger(String sText) {
        // 合法字元集：僅包含數字，不含小數點或負號
        String ValidChars = "0123456789";
        boolean IsInteger = true;
        char sChar;

        for (int i = 0; i < sText.length() && IsInteger == true; i++) {
            sChar = sText.charAt(i);
            if (ValidChars.indexOf(sChar) == -1) {
                IsInteger = false;
            }
        }
        return IsInteger;
    }

    /**
     * 將字串以 byte 長度為基準，在右側補空白至指定長度。
     * <p>
     * 與 {@link StringUtils#rightPad} 不同，此方法使用 byte 陣列操作，
     * 因此能正確處理含有中文字的字串（中文字佔 2 byte）。
     * 若 {@code body} 的 byte 長度超過 {@code length}，則會截斷到 {@code length}。
     * </p>
     *
     * @param body   原始字串
     * @param length 目標 byte 長度
     * @return 右側補空白後的字串
     */
    public static String stringRightPad(String body, int length) {
        if (StringUtils.isNotBlank(body)) {
            // 計算需要補充的空白 byte 數，若原字串已超過目標長度則補 0 個
            byte[] blankByte = ((length - body.getBytes().length) > 0 ? new byte[(length - body.getBytes().length)] : new byte[0]);
            Arrays.fill(blankByte, _STRING_BYTE_BLANK);
            return new String(ArrayUtils.subarray(body.getBytes(), 0, length)) + new String(blankByte);
        }
        return StringUtils.rightPad(_STRING_BLANK, length, _STRING_BLANK);
    }

    /**
     * 將字串在左側補空白至指定長度。
     * <p>
     * 若 {@code body} 的字元長度超過 {@code length}，會先截斷再補位；
     * 若 {@code body} 為空白則回傳全部由空白組成的字串。
     * </p>
     *
     * @param body   原始字串
     * @param length 目標字元長度
     * @return 左側補空白後的字串
     */
    public static String stringLeftPad(String body, int length) {
        if (StringUtils.isNotBlank(body)) {
            String bodyString = String.valueOf(body);
            // 若字串長度超過目標長度，先截斷再補位，避免結果超出指定長度
            return StringUtils.leftPad(bodyString.substring(0,
                    (bodyString.length() <= length ? bodyString.length() : length)), length, _STRING_BLANK);
        }
        return StringUtils.leftPad(_STRING_BLANK, length, _STRING_BLANK);
    }

    /**
     * 將字串在左側補 "0" 至指定長度。
     * <p>
     * 若 {@code body} 的字元長度超過 {@code length}，會先截斷再補位；
     * 若 {@code body} 為空白則回傳全部由 "0" 組成的字串。
     * </p>
     *
     * @param body   原始字串
     * @param length 目標字元長度
     * @return 左側補零後的字串
     */
    public static String stringLeftZero(String body, int length) {
        if (StringUtils.isNotBlank(body)) {
            String bodyString = String.valueOf(body);
            // 若字串長度超過目標長度，先截斷再補位，避免結果超出指定長度
            return StringUtils.leftPad(bodyString.substring(0,
                    (bodyString.length() <= length ? bodyString.length() : length)), length, "0");
        }
        return StringUtils.leftPad("0", length, "0");
    }

    /**
     * 從字串中取出部分字串,會考慮到中文字的處理
     *
     * @param source 原始字串
     * @param start  開始位置(從0起算)
     * @param end    結束位置
     * @return 部分字串
     * @throws Exception
     */
    public static String getSubstring(String source, int start, int end) {
        String resultStr = "";
        if (source == null || start < 0 || start >= end || end < 0) {
            return resultStr;
        }
        try {
            byte[] byteArray = source.getBytes();
            if (end > byteArray.length) {
                return resultStr;
            }
            //不含中文字
            if (byteArray.length == source.length()) {
                resultStr = StringUtils.substring(source, start, end);
            } else {
                //含有中文字
                resultStr = new String(ArrayUtils.subarray(byteArray, start, end));
            }
        } catch (Exception e) {
            throw e;
            //System.out.println("getSubstring() Exception=\r\n" + e.toString());
        }
        return resultStr;
    }

    /**
     * 從字串中取出部分字串(從開始到最後),會考慮到中文字的處理
     *
     * @param source 原始字串
     * @param start  開始位置(從0起算)
     * @return 部分字串
     * @throws Exception
     */
    public static String getSubstring(String source, int start) {
        String resultStr = "";
        if (source == null || start < 0) {
            return resultStr;
        }
        try {
            byte[] byteArray = source.getBytes();
            //不含中文字
            if (byteArray.length == source.length()) {
                resultStr = StringUtils.substring(source, start);
            } else {
                //含有中文字
                resultStr = new String(ArrayUtils.subarray(byteArray, start, byteArray.length - 1));
            }
        } catch (Exception e) {
            throw e;
        }
        return resultStr;
    }

    /**
     * 將字串長度不足的部分補空白
     *
     * @param source        原始字串
     * @param requestLength 要求長度
     * @return 補足長度的字串
     */
    public static String fillWithSpace(String source, int requestLength) {
        if (source == null) {
            source = "";
        }
        if (requestLength < 0) {
            requestLength = 0;
        }
        //原始字串長度(用byte方式計算)
        int sourceLength = source.getBytes().length;
        //長度相同
        if (sourceLength == requestLength) {
            return source;
        }

        String result = source;
        //40個空白
        String space = "                                        ";
        //長度太短
        if (sourceLength < requestLength) {
            //不足的長度
            int moreLength = requestLength - sourceLength;
            while (moreLength > space.length()) {
                space += space;
            }
            result = source + space.substring(0, moreLength);
        }
        //長度太長
        if (sourceLength > requestLength) {
            byte[] byteArray = source.getBytes();
            result = new String(ArrayUtils.subarray(byteArray, 0, requestLength));
        }

        return result;
    }

    /**
     * 將數字長度不足的部分補空白
     *
     * @param b             原始字串
     * @param requestLength 要求長度
     * @return 補足長度的字串
     */
    public static String fillWithSpaceByNumber(BigDecimal b, int requestLength) {
        String source = "";
        if (b != null) {
            source = b.toString();
            if (!NumberUtils.isDigits(b.abs().toString())) {
                StringBuffer reverse = new StringBuffer(source).reverse();
                while (reverse.toString().startsWith("0")) {
                    reverse = new StringBuffer(StringUtils.chomp(reverse.reverse().toString(), "0")).reverse();
                }
                if (reverse.toString().startsWith(".")) {
                    reverse.deleteCharAt(0);
                }
                source = reverse.reverse().toString();
            }
        }
        return StringUtils.leftPad(source, requestLength);
    }

    /**
     * 將字串的第一個字元轉為小寫，其餘字元保持不變。
     * <p>
     * 若 {@code string} 為 {@code null} 或空字串，則回傳空字串 {@code ""}。
     * </p>
     *
     * @param string 要轉換的字串
     * @return 首字母轉小寫後的字串；若為 {@code null} 或空字串則回傳 {@code ""}
     */
    public static String lowerCaseForFirstLetter(String string) {
        return string == null || string.isEmpty() ? "" : Character.toLowerCase(string.charAt(0)) + string.substring(1);
    }
}
