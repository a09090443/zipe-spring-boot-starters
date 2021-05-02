package com.zipe.model;

import lombok.Data;

import java.util.Date;

@Data
public class User {

    private Long userId;

    private String username;

    private String email;

    private Date gmtCreate;
}
