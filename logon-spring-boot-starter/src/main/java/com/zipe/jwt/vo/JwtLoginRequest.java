package com.zipe.jwt.vo;

import lombok.Data;

/** JWT 登入請求。 */
@Data
public class JwtLoginRequest {
    private String username;
    private String password;
}
