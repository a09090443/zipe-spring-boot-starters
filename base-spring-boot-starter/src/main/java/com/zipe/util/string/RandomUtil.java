package com.zipe.util.string;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 亂數字串產生工具。
 * <p>
 * 提供以下常用的隨機字串產生功能：
 * <ul>
 *   <li>英數混合隨機字串</li>
 *   <li>純字母（大小寫混合、全大寫、全小寫）隨機字串</li>
 *   <li>固定長度補零字串</li>
 *   <li>數字左側補零至指定長度的字串</li>
 * </ul>
 * 內部使用 {@link java.security.SecureRandom} 以提高隨機性安全強度，
 * 適用於產生驗證碼、臨時密碼、流水號等場景。
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2020/11/4 下午 03:30
 **/
public class RandomUtil {
    /** 英數字元集合：數字 0-9 加上大小寫英文字母，共 62 個字元。 */
    public static final String allChar = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    /** 純英文字母字元集合：大小寫各 26 個字元，共 52 個字元。 */
    public static final String letterChar = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    /** 純數字字元集合：0-9，共 10 個字元。 */
    public static final String numberChar = "0123456789";

    /**
     * 產生指定長度的隨機英數混合字串（包含數字 0-9 及大小寫英文字母）。
     *
     * @param length 要產生的字串長度
     * @return 由 {@link #allChar} 字元集合隨機組成的字串
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
     * 傳回指定長度的隨機字母字串（大小寫混合，不含數字）。
     *
     * @param length 隨機字串長度
     * @return 由 {@link #letterChar} 字元集合隨機組成的大小寫混合字串
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
     * 傳回指定長度的隨機純小寫字母字串。
     * <p>
     * 以 {@link #generateMixStr(int)} 產生大小寫混合字串後，再統一轉為小寫。
     * </p>
     *
     * @param length 隨機字串長度
     * @return 全部小寫的隨機字母字串
     */
    public static String generateLowerStr(int length) {
        return generateMixStr(length).toLowerCase();
    }

    /**
     * 傳回指定長度的隨機純大寫字母字串。
     * <p>
     * 以 {@link #generateMixStr(int)} 產生大小寫混合字串後，再統一轉為大寫。
     * </p>
     *
     * @param length 隨機字串長度
     * @return 全部大寫的隨機字母字串
     */
    public static String generateUpperStr(int length) {
        return generateMixStr(length).toUpperCase();
    }

    /**
     * 產生指定長度、全由字元 '0' 組成的字串，常用於左側補零的前置作業。
     *
     * @param length 字串長度
     * @return 由 length 個 '0' 組成的字串
     */
    public static String generateZeroStr(int length) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            sb.append('0');
        }
        return sb.toString();
    }

    /**
     * 將數字轉換為指定長度的字串，長度不足時於左側補 '0'。
     * <p>
     * 例如 {@code toFixedLengthStr(42, 6)} 傳回 {@code "000042"}。
     * </p>
     *
     * @param num       要轉換的數字
     * @param fixdlenth 目標字串長度
     * @return 左側補零後達到 fixdlenth 長度的數字字串
     * @throws RuntimeException 若數字本身的位數已超過 fixdlenth，則拋出此例外
     */
    public static String toFixedLengthStr(long num, int fixdlenth) {
        StringBuffer sb = new StringBuffer();
        String strNum = String.valueOf(num);
        // 計算需要補零的位數；若數字本身位數超過目標長度則無法補零，直接拋出例外
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
