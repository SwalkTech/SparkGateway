package com.spark.gateway.core.filter.gray.strategy;

import com.spark.gateway.config.pojo.ServiceInstance;
import com.spark.gateway.core.context.GatewayContext;

import java.util.List;

public interface GrayStrategy {

    boolean shouldRoute2Gray(GatewayContext context, List<ServiceInstance> instances);

    String mark();

}
