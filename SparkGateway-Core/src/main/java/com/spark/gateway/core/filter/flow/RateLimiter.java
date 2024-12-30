package com.spark.gateway.core.filter.flow;

import com.spark.gateway.core.context.GatewayContext;

public interface RateLimiter {

    void tryConsume(GatewayContext context);

}
