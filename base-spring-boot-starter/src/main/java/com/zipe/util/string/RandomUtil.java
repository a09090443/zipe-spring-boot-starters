package com.zipe.util.string;

import java.security.SecureRandom;
import java.util.Random;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/11/4 下午 03:30
 **/
public class RandomUtil {
    public static final String allChar = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String letterChar = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String numberChar = "0123456789";

    /**
     * 產生len長度的隨機字串
     *
     * @param length
     * @return
     */
    public static String generateStr(int length) {
        StringBuffer sb = new StringBuffer();
        Random random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            sb.append(allChar.charAt(random.nextInt(allChar.length())));
        }
        return sb.toString();
    }

    /**
     * 返回一個定長的隨機純字母字串(只包含大小寫字母)
     *
     * @param length 隨機字串長度
     * @return 隨機字串
     */
    public static String generateMixStr(int length) {
        StringBuffer sb = new StringBuffer();
        Random random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            sb.append(letterChar.charAt(random.nextInt(letterChar.length())));
        }
        return sb.toString();
    }

    /**
     * 返回一個定長的隨機純大寫字母字串(只包含大小寫字母)
     *
     * @param length 隨機字串長度
     * @return 隨機字串
     */
    public static String generateLowerStr(int length) {
        return generateMixStr(length).toLowerCase();
    }

    /**
     * 返回一個定長的隨機純小寫字母字串(只包含大小寫字母)
     *
     * @param length 隨機字串長度
     * @return 隨機字串
     */
    public static String generateUpperStr(int length) {
        return generateMixStr(length).toUpperCase();
    }

    /**
     * 生成一個定長的純0字串
     *
     * @param length 字串長度
     * @return 純0字串
     */
    public static String generateZeroStr(int length) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            sb.append('0');
        }
        return sb.toString();
    }

    /**
     * 根據數字生成一個定長的字串,長度不夠前面補0
     *
     * @param num       數字
     * @param fixdlenth 字串長度
     * @return 定長的字串
     */
    public static String toFixedLengthStr(long num, int fixdlenth) {
        StringBuffer sb = new StringBuffer();
        String strNum = String.valueOf(num);
        if (fixdlenth - strNum.length() >= 0) {
            sb.append(generateZeroStr(fixdlenth - strNum.length()));
        } else {
            throw new RuntimeException("將數字" + num + "轉化為長度為" + fixdlenth + "的字串發生異常!");
        }
        sb.append(strNum);
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(generateStr(4));
    }
}
