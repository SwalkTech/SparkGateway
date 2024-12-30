package com.spark.gateway.core.resilience.fallback;

import com.spark.gateway.core.context.GatewayContext;

public interface FallbackHandler {

    void handle(Throwable throwable, GatewayContext context);

    String mark();

}
