package com.zipe.util.string;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Arrays;

public class CommonStringUtil {

    public static String NUMBER_FORMAT0 = "#";
    public static String NUMBER_FORMAT1 = "###,###";
    public static String NUMBER_FORMAT2 = "###,###.00";
    public static String NUMBER_FORMAT3 = "###,##0.00";

    public static final byte _STRING_BYTE_BLANK = " ".getBytes()[0];
    public static final String _STRING_BLANK = "";

    /**
     * 將數字字串 轉為***,***.**的格式
     *
     * @param number
     * @param numberFormat
     * @return
     */
    public static String numberFormat(String number, String numberFormat) {
        DecimalFormat decFormat = new DecimalFormat(numberFormat);
        if (StringUtils.isBlank(number) || number.equals("null")) {
            number = "0";
        }
        String resultStr = decFormat.format(new BigDecimal(number));
        return resultStr;
    }

    /**
     * num = 傳入的值，len = 總長度
     *
     * @param num
     * @param len
     * @return
     */
    public static String addZero(Integer num, int len) {
        String newStr = "";
        String str = String.valueOf(num.intValue());
        int strLength = str.length();
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
     * num = 傳入的值，len = 總長度
     *
     * @param num
     * @param len
     * @return
     */
    public static String addZero(int num, int len) {
        String newStr = "";
        String str = String.valueOf(num);
        int strLength = str.length();
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
     * 取得字串長度,中文兩碼,英文一碼
     *
     * @param str
     * @return
     */
    public static int getStringLength(String str) {
        return str.getBytes().length;
    }


    /**
     * 檢查是否為數字
     *
     * @param sText
     * @return 是數字：true，不是數字：false
     */
    public static boolean isNumber(String sText) {
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
     * 檢查是否為整數
     *
     * @param sText
     * @return 是整數：true，不是整數：false
     */
    public static boolean isInteger(String sText) {
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
     * 字串右補空白
     *
     * @param body
     * @param length
     * @return
     * @throws Exception
     */
    public static String stringRightPad(String body, int length) {
        if (StringUtils.isNotBlank(body)) {
            byte[] blankByte = ((length - body.getBytes().length) > 0 ? new byte[(length - body.getBytes().length)] : new byte[0]);
            Arrays.fill(blankByte, _STRING_BYTE_BLANK);
            return new String(ArrayUtils.subarray(body.getBytes(), 0, length)) + new String(blankByte);
        }
        return StringUtils.rightPad(_STRING_BLANK, length, _STRING_BLANK);
    }

    /**
     * 字串左補空白
     *
     * @param body
     * @param length
     * @return
     * @throws Exception
     */
    public static String stringLeftPad(String body, int length) {
        if (StringUtils.isNotBlank(body)) {
            String bodyString = String.valueOf(body);
            return StringUtils.leftPad(bodyString.substring(0,
                    (bodyString.length() <= length ? bodyString.length() : length)), length, _STRING_BLANK);
        }
        return StringUtils.leftPad(_STRING_BLANK, length, _STRING_BLANK);
    }

    /**
     * 字串左補0
     *
     * @param body
     * @param length
     * @return
     * @throws Exception
     */
    public static String stringLeftZero(String body, int length) {
        if (StringUtils.isNotBlank(body)) {
            String bodyString = String.valueOf(body);
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
     * 將字串第一個英文字轉為小寫
     *
     * @param string
     * @return
     */
    public static String lowerCaseForFirstLetter(String string) {
        return string == null || string.isEmpty() ? "" : Character.toLowerCase(string.charAt(0)) + string.substring(1);
    }
}
