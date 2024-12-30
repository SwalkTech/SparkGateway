package com.spark.gateway.core.filter.loadbalance.strategy;

import com.spark.gateway.config.pojo.ServiceInstance;
import com.spark.gateway.core.context.GatewayContext;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.spark.constant.LoadBalanceConstant.RANDOM_LOAD_BALANCE_STRATEGY;

public class RandomLoadBalanceStrategy implements LoadBalanceStrategy {

    /**
     * 选择实例方法
     * 该方法用于在给定的服务实例列表中选择一个实例
     * 选择是通过随机方式完成的，以简单的方式实现负载均衡
     *
     * @param context   网关上下文，包含请求的上下文信息，未在本方法中使用，但可能在扩展或上下文敏感的实现中使用
     * @param instances 服务实例列表，表示当前可用的服务实例集合
     * @return 随机选择的服务实例，用于处理即将到来的请求
     */
    @Override
    public ServiceInstance selectInstance(GatewayContext context, List<ServiceInstance> instances) {
        // 使用ThreadLocalRandom生成随机数，以选择一个随机实例
        // 这种方式确保了选择的随机性和简单性，适用于不考虑实例负载和性能差异的场景
        return instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
    }

    @Override
    public String mark() {
        return RANDOM_LOAD_BALANCE_STRATEGY;
    }

}
