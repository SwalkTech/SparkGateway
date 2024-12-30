package com.spark.exception;



import com.spark.enums.ResponseCode;

import java.io.Serial;


/**
 * @author: Xlihua
 * @date: 2024/12/27 12:42
 * @description: 响应异常
 **/
public class ResponseException extends GatewayException {

    @Serial
    private static final long serialVersionUID = 707018357827678269L;

    public ResponseException(ResponseCode code) {
        super(code.getMessage(), code);
    }

    public ResponseException(Throwable cause, ResponseCode code) {
        super(code.getMessage(), cause, code);
        this.code = code;
    }

}
