package com.spark.exception;




import com.spark.enums.ResponseCode;

import java.io.Serial;


/**
 * @author: spark
 * @date: 2024/12/27 12:45
 * @description: 限制异常，一般发生在流控
 **/
public class LimitedException extends GatewayException {

    @Serial
    private static final long serialVersionUID = -5975157585816767314L;

    public LimitedException(ResponseCode code) {
        super(code.getMessage(), code);
    }

    public LimitedException(Throwable cause, ResponseCode code) {
        super(code.getMessage(), cause, code);
    }

}
