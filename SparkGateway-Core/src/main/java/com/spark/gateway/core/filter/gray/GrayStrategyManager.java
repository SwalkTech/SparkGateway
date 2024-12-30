package com.spark.gateway.core.filter.gray;

import com.spark.gateway.core.filter.gray.strategy.SparkStrategy;
import com.spark.gateway.core.filter.gray.strategy.ThresholdGrayStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;


@Slf4j
public class GrayStrategyManager {

    private static final Map<String, SparkStrategy> strategyMap = new HashMap<>();

    static {
        ServiceLoader<SparkStrategy> serviceLoader = ServiceLoader.load(SparkStrategy.class);
        for (SparkStrategy strategy : serviceLoader) {
            strategyMap.put(strategy.mark(), strategy);
            log.info("load gray strategy success: {}", strategy);
        }
    }

    /**
     * 根据名称获取灰度策略
     * 如果给定名称对应的灰度策略不存在，则返回一个默认的阈值灰度策略
     *
     * @param name 灰度策略的名称
     * @return 对应名称的灰度策略实例，如果不存在则返回ThresholdGrayStrategy实例
     */
    public static SparkStrategy getStrategy(String name) {
        // 从策略映射中获取指定名称的灰度策略
        SparkStrategy strategy = strategyMap.get(name);
        // 如果未找到指定名称的灰度策略，则创建并返回一个默认的阈值灰度策略
        if (strategy == null) {
            strategy = new ThresholdGrayStrategy();
        }
        // 返回找到或新创建的灰度策略
        return strategy;
    }

}
