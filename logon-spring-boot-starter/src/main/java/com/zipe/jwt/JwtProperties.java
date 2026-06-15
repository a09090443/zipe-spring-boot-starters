package com.zipe.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 驗證相關設定屬性。
 *
 * <p>對應設定檔前綴 {@code security.jwt}，提供演算法、金鑰、有效期限與
 * token 標頭等設定。預設使用 HS256 對稱式演算法。</p>
 */
@Data
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /** 簽章演算法，支援 {@code HS256}（對稱）與 {@code RS256}（非對稱）。 */
    private String algorithm = "HS256";

    /** HS256 對稱式密鑰，長度至少需 32 位元組。 */
    private String secret;

    /** RS256 私鑰位置（PEM 格式），供發行 token 簽章使用。 */
    private String privateKeyLocation;

    /** RS256 公鑰位置（PEM 格式），供驗證 token 簽章使用。 */
    private String publicKeyLocation;

    /** token 有效期限（秒），預設 3600 秒（1 小時）。 */
    private long expirationSeconds = 3600L;

    /** 內建登入端點 URI。 */
    private String loginUri = "/api/login";

    /** 攜帶 token 的 HTTP 標頭名稱。 */
    private String header = "Authorization";

    /** token 前綴字串。 */
    private String tokenPrefix = "Bearer ";

    /** token 簽發者（iss claim），選填。 */
    private String issuer;
}
