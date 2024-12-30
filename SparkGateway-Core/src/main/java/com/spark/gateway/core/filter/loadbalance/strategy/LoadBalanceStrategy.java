package com.spark.gateway.core.filter.loadbalance.strategy;

import com.spark.gateway.config.pojo.ServiceInstance;
import com.spark.gateway.core.context.GatewayContext;

import java.util.List;

public interface LoadBalanceStrategy {

    ServiceInstance selectInstance(GatewayContext context, List<ServiceInstance> instances);

    String mark();

}
