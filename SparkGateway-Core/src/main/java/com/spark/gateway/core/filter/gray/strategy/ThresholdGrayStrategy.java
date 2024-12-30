package com.spark.gateway.core.filter.gray.strategy;

import com.spark.gateway.config.pojo.RouteDefinition;
import com.spark.gateway.config.pojo.ServiceInstance;
import com.spark.gateway.config.util.FilterUtil;
import com.spark.gateway.core.context.GatewayContext;

import java.util.List;

import static com.spark.constant.FilterConstant.SPARK_FILTER_NAME;
import static com.spark.constant.SparkConstant.MAX_GRAY_THRESHOLD;
import static com.spark.constant.SparkConstant.THRESHOLD_GRAY_STRATEGY;

/**
 * 根据流量决定是否灰度策略
 */
public class ThresholdGrayStrategy implements SparkStrategy {

    @Override
    public boolean shouldRoute2Gray(GatewayContext context, List<ServiceInstance> instances) {
        if (instances.stream().anyMatch(instance -> instance.isEnabled() && !instance.isGray())) {
            RouteDefinition.GrayFilterConfig grayFilterConfig = FilterUtil.findFilterConfigByClass(context.getRoute().getFilterConfigs(), SPARK_FILTER_NAME, RouteDefinition.GrayFilterConfig.class);
            double maxGrayThreshold = grayFilterConfig == null ? MAX_GRAY_THRESHOLD : grayFilterConfig.getMaxGrayThreshold();
            double grayThreshold = instances.stream().mapToDouble(ServiceInstance::getThreshold).sum();
            grayThreshold = Math.min(grayThreshold, maxGrayThreshold);
            return Math.abs(Math.random() - 1) <= grayThreshold;
        }
        return true;
    }

    @Override
    public String mark() {
        return THRESHOLD_GRAY_STRATEGY;
    }

}
