package com.spark.gateway.core.filter.loadbalance.strategy;

import com.spark.gateway.config.pojo.ServiceInstance;
import com.spark.gateway.core.context.GatewayContext;

import java.util.List;

import static com.spark.constant.LoadBalanceConstant.CLIENT_IP_LOAD_BALANCE_STRATEGY;

public class ClientIpLoadBalanceStrategy implements LoadBalanceStrategy {

    /**
     * 根据请求的主机名选择服务实例
     * 本方法旨在实现一种简单的负载均衡策略，通过计算请求主机名的哈希值来决定选择哪个服务实例
     * 这种方法可以确保相同主机名的请求总是被路由到同一个服务实例，直到服务实例列表发生变化
     *
     * @param context   网关上下文，包含请求和响应的相关信息
     * @param instances 服务实例列表，代表可选的服务节点
     * @return 返回选定的服务实例
     */
    @Override
    public ServiceInstance selectInstance(GatewayContext context, List<ServiceInstance> instances) {
        // 根据请求主机名的哈希值选择服务实例
        // 使用Math.abs确保哈希值为非负数，以防止索引出错
        // 使用模运算确保结果在实例列表大小范围内，实现循环选择
        return instances.get(Math.abs(context.getRequest().getHost().hashCode()) % instances.size());
    }

    @Override
    public String mark() {
        return CLIENT_IP_LOAD_BALANCE_STRATEGY;
    }

}
