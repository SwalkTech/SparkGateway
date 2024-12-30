package com.spark.exception;




import com.spark.enums.ResponseCode;

import java.io.Serial;



/**
 * @author: spark
 * @date: 2024/12/27 12:44
 * @description: 当下游服务找不到时抛出该异常
 **/
public class NotFoundException extends GatewayException {

    @Serial
	private static final long serialVersionUID = -4825153388389722853L;

    public NotFoundException(ResponseCode code) {
        super(code.getMessage(), code);
    }

    public NotFoundException(Throwable cause, ResponseCode code) {
        super(code.getMessage(), cause, code);
    }

}
