package com.spark.gateway.core.filter.gray.strategy;

import com.spark.gateway.config.pojo.RouteDefinition;
import com.spark.gateway.config.pojo.ServiceInstance;
import com.spark.gateway.config.util.FilterUtil;
import com.spark.gateway.core.context.GatewayContext;

import java.util.List;

import static com.spark.constant.FilterConstant.SPARK_FILTER_NAME;
import static com.spark.constant.SparkConstant.CLIENT_IP_GRAY_STRATEGY;


public class ClientIpGrayStrategy implements GrayStrategy {

    @Override
    public boolean shouldRoute2Gray(GatewayContext context, List<ServiceInstance> instances) {
        if (instances.stream().anyMatch(instance -> instance.isEnabled() && !instance.isGray())) {
            RouteDefinition.GrayFilterConfig grayFilterConfig = FilterUtil.findFilterConfigByClass(context.getRoute().getFilterConfigs(), SPARK_FILTER_NAME, RouteDefinition.GrayFilterConfig.class);
            double grayThreshold = instances.stream().mapToDouble(ServiceInstance::getThreshold).sum();
            grayThreshold = Math.min(grayThreshold, grayFilterConfig.getMaxGrayThreshold());
            return Math.abs(context.getRequest().getHost().hashCode()) % 100 <= grayThreshold * 100;
        }
        return true;
    }

    @Override
    public String mark() {
        return CLIENT_IP_GRAY_STRATEGY;
    }

}
