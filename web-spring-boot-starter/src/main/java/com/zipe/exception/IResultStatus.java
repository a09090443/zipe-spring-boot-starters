package com.zipe.exception;

import org.springframework.http.HttpStatus;

public interface IResultStatus {
    HttpStatus getHttpStatus();

    Integer getCode();

    String getMessage();
}
