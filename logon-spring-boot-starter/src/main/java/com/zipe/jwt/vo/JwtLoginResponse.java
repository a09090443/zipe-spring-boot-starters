package com.zipe.jwt.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** JWT 登入回應。 */
@Data
@AllArgsConstructor
public class JwtLoginResponse {
    private String token;
    private String tokenType;
}
