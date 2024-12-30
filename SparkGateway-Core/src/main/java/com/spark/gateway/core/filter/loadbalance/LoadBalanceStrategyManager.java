package com.spark.gateway.core.filter.loadbalance;

import com.spark.gateway.core.filter.loadbalance.strategy.LoadBalanceStrategy;
import com.spark.gateway.core.filter.loadbalance.strategy.RoundRobinLoadBalanceStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;


@Slf4j
public class LoadBalanceStrategyManager {

    private static final Map<String, LoadBalanceStrategy> strategyMap = new HashMap<>();

    static {
        ServiceLoader<LoadBalanceStrategy> serviceLoader = ServiceLoader.load(LoadBalanceStrategy.class);
        for (LoadBalanceStrategy strategy : serviceLoader) {
            strategyMap.put(strategy.mark(), strategy);
            log.info("load loadbalance strategy success: {}", strategy);
        }
    }

    /**
     * 根据名称获取负载均衡策略
     * 如果策略映射中不存在该名称对应的策略，则使用默认的轮询负载均衡策略
     *
     * @param name 策略名称
     * @return 对应名称的负载均衡策略，如果不存在则返回默认的轮询负载均衡策略
     */
    public static LoadBalanceStrategy getStrategy(String name) {
        // 从策略映射中根据名称获取对应的负载均衡策略
        LoadBalanceStrategy strategy = strategyMap.get(name);
        // 如果未找到对应的策略，使用默认的轮询负载均衡策略
        if (strategy == null) {
            strategy = new RoundRobinLoadBalanceStrategy();
        }
        // 返回负载均衡策略
        return strategy;
    }

}
