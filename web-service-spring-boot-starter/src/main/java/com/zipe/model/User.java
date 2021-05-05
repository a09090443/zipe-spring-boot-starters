package com.zipe.model;

import java.io.Serializable;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/5/4 下午 02:04
 **/
public class User implements Serializable {
    private static final long serialVersionUID = -3628469724795296287L;

    private String userId;
    private String userName;
    private String email;


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
