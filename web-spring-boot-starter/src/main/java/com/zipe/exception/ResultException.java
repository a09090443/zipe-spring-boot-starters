package com.zipe.exception;

import com.zipe.enums.ResultStatus;
import lombok.Getter;

@Getter
public class ResultException extends Exception {

    /**
     * 異常資訊訊息
     */
    ResultStatus resultStatus;

    public ResultException() {
        this(ResultStatus.INTERNAL_SERVER_ERROR);
    }

    public ResultException(ResultStatus resultStatus) {
        super(resultStatus.getMessage());
        this.resultStatus = resultStatus;
    }
}