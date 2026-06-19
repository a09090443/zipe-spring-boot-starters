package com.zipe.autoconfiguration;

import com.zipe.config.SecurityPropertyConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 驗證模組預設 {@code passwordEncoder} Bean 的相容性：
 * 既要支援帶 {@code {id}} 前綴的密碼，又要相容既有「無前綴的純 BCrypt 雜湊」
 * （守護 CUSTOM 模式以 {@code passwordEncoder.matches} 比對舊資料庫雜湊不被破壞）。
 */
class PasswordEncoderConfigTest {

    private PasswordEncoder encoder() {
        return new SecurityConfiguration(new SecurityPropertyConfig()).passwordEncoder();
    }

    /**
     * 既有資料庫中常見的「無 {@code {id}} 前綴」BCrypt 雜湊（如 {@code $2a$10$...}）
     * 必須仍能比對成功，否則升級後 CUSTOM 模式登入會全面失效。
     */
    @Test
    void matchesLegacyUnprefixedBcryptHash() {
        String legacyHash = new BCryptPasswordEncoder().encode("1234");
        assertTrue(encoder().matches("1234", legacyHash), "無前綴的舊 BCrypt 雜湊應可比對成功");
    }

    /**
     * 帶 {@code {bcrypt}} 前綴的密碼亦須能比對成功。
     */
    @Test
    void matchesPrefixedBcryptHash() {
        PasswordEncoder encoder = encoder();
        String prefixed = encoder.encode("1234"); // 產生帶 {bcrypt} 前綴的雜湊
        assertTrue(prefixed.startsWith("{bcrypt}"), "預設應以 {bcrypt} 前綴編碼");
        assertTrue(encoder.matches("1234", prefixed), "帶前綴的雜湊應可比對成功");
    }

    /**
     * 以 starters_example 的真實種子雜湊（無前綴 {@code $2a$10$...}）驗證 CUSTOM 模式登入：
     * {@code user01/1234} 與 {@code user02/abcd} 須能比對成功，守護升級後範例 custom 登入不被破壞。
     */
    @Test
    void matchesExampleSeedBcryptHashes() {
        PasswordEncoder encoder = encoder();
        String user01Hash = "$2a$10$Y6WAl60GuH2wIULKsaRotuHGCAoYfGXmvclCEO2PrvRNQIqcb0VB2";
        String user02Hash = "$2a$10$1Ihk8NP/mi1bxAErFUA0fu6RnY0EnuDEqYSa57VkDvxzgTrDNMgoK";
        assertTrue(encoder.matches("1234", user01Hash), "user01/1234 應可比對成功");
        assertTrue(encoder.matches("abcd", user02Hash), "user02/abcd 應可比對成功");
    }
}
