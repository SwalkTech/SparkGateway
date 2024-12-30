package com.spark.gateway.core.filter.gray.strategy;

import com.spark.gateway.config.pojo.ServiceInstance;
import com.spark.gateway.core.context.GatewayContext;

import java.util.List;

/**
 * @author: spark
 * @date: 2024/12/30 10:29
 * @description: 灰度策略
 **/
public interface SparkStrategy {

    boolean shouldRoute2Gray(GatewayContext context, List<ServiceInstance> instances);

    String mark();

}
