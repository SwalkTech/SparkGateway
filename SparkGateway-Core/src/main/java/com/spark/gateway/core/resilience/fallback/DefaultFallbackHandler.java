package com.spark.gateway.core.resilience.fallback;

import com.spark.gateway.common.enums.ResponseCode;
import com.spark.gateway.core.context.GatewayContext;
import com.spark.gateway.core.helper.ContextHelper;
import com.spark.gateway.core.helper.ResponseHelper;

import static com.spark.gateway.common.constant.FallbackConstant.DEFAULT_FALLBACK_HANDLER_NAME;

public class DefaultFallbackHandler implements FallbackHandler {

    @Override
    public void handle(Throwable throwable, GatewayContext context) {
        context.setThrowable(throwable);
        context.setResponse(ResponseHelper.buildGatewayResponse(ResponseCode.GATEWAY_FALLBACK));
        ContextHelper.writeBackResponse(context);
    }

    @Override
    public String mark() {
        return DEFAULT_FALLBACK_HANDLER_NAME;
    }

}
