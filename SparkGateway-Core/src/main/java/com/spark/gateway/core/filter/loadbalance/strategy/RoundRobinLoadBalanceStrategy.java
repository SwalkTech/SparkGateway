package com.spark.gateway.core.filter.loadbalance.strategy;

import com.spark.gateway.config.pojo.RouteDefinition;
import com.spark.gateway.config.pojo.ServiceInstance;
import com.spark.gateway.config.util.FilterUtil;
import com.spark.gateway.core.context.GatewayContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.spark.constant.FilterConstant.LOAD_BALANCE_FILTER_NAME;
import static com.spark.constant.LoadBalanceConstant.ROUND_ROBIN_LOAD_BALANCE_STRATEGY;

public class RoundRobinLoadBalanceStrategy implements LoadBalanceStrategy {

    Map<String, AtomicInteger> strictPositionMap = new ConcurrentHashMap<>();

    Map<String, Integer> positionMap = new ConcurrentHashMap<>();

    private final int THRESHOLD = Integer.MAX_VALUE >> 2; // 预防移除的安全阈值

    /**
     * 使用轮询算法选择服务实例
     *
     * @param context   网关上下文，包含路由和请求等信息
     * @param instances 服务实例列表
     * @return 选定的服务实例
     */
    @Override
    public ServiceInstance selectInstance(GatewayContext context, List<ServiceInstance> instances) {
        // 默认采用严格的轮询策略
        boolean isStrictRoundRobin = true;
        // 尝试从路由定义中获取负载均衡过滤器配置
        RouteDefinition.LoadBalanceFilterConfig loadBalanceFilterConfig = FilterUtil.findFilterConfigByClass(context.getRoute().getFilterConfigs(), LOAD_BALANCE_FILTER_NAME, RouteDefinition.LoadBalanceFilterConfig.class);
        if (loadBalanceFilterConfig != null) {
            // 如果配置存在，则使用配置中的轮询策略
            isStrictRoundRobin = loadBalanceFilterConfig.isStrictRoundRobin();
        }
        // 获取服务名称
        String serviceName = context.getRequest().getServiceDefinition().getServiceName();
        ServiceInstance serviceInstance;
        // 根据是否是严格的轮询策略来选择实例
        if (isStrictRoundRobin) {
            // 使用AtomicInteger来保证线程安全地更新位置
            AtomicInteger strictPosition = strictPositionMap.computeIfAbsent(serviceName, k -> new AtomicInteger(0));
            // 获取并更新位置
            int index = Math.abs(strictPosition.getAndIncrement());
            // 选择实例
            serviceInstance = instances.get(index % instances.size());
            // 如果达到阈值，则重置位置
            if (index >= THRESHOLD) {
                strictPosition.set((index + 1) % instances.size());
            }
        } else {
            // 非严格模式下，使用普通的int来记录位置
            int position = positionMap.getOrDefault(serviceName, 0);
            // 计算位置
            int index = Math.abs(position++);
            // 选择实例
            serviceInstance = instances.get(index % instances.size());
            // 如果达到阈值，则重置位置
            if (position >= THRESHOLD) {
                positionMap.put(serviceName, (position + 1) % instances.size());
            }
        }
        // 返回选定的实例
        return serviceInstance;
    }

    @Override
    public String mark() {
        return ROUND_ROBIN_LOAD_BALANCE_STRATEGY;
    }

}
