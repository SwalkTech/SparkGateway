package com.spark.exception;


import com.spark.enums.ResponseCode;
import lombok.Getter;

import java.io.Serial;

/**
 * @author: spark
 * @date: 2024/12/27 12:44
 * @description: 网关异常
 **/
@Getter
public class GatewayException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -1159027826621990252L;
    protected ResponseCode code;

    public GatewayException() {
    }

    public GatewayException(String message, ResponseCode code) {
        super(message);
        this.code = code;
    }

    public GatewayException(String message, Throwable cause, ResponseCode code) {
        super(message, cause);
        this.code = code;
    }

}
