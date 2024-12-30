package com.spark.gateway.core.filter.loadbalance.strategy;

import com.spark.gateway.config.pojo.ServiceInstance;
import com.spark.gateway.core.context.GatewayContext;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.spark.constant.LoadBalanceConstant.WEIGHT_LOAD_BALANCE_STRATEGY;


public class WeightLoadBalanceStrategy implements LoadBalanceStrategy {

    /**
     * 根据服务实例的权重选择一个实例
     * 权重越高，被选中的概率越大
     * 如果所有实例的权重之和小于等于0，则返回null
     *
     * @param context   网关上下文，包含请求和响应等信息
     * @param instances 服务实例列表，从中选择一个实例
     * @return 根据权重随机选择的服务实例，如果没有合适的实例则返回null
     */
    @Override
    public ServiceInstance selectInstance(GatewayContext context, List<ServiceInstance> instances) {
        // 计算所有实例的权重之和
        int totalWeight = instances.stream().mapToInt(ServiceInstance::getWeight).sum();
        // 如果权重之和小于等于0，表示所有实例都不适合，直接返回null
        if (totalWeight <= 0) {
            return null;
        }
        // 生成一个随机权重，用于在实例中按权重随机选择
        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);
        // 遍历实例列表，减去随机权重直到小于0，表示找到合适的实例
        for (ServiceInstance instance : instances) {
            randomWeight -= instance.getWeight();
            // 当随机权重小于0时，返回当前实例
            if (randomWeight < 0) {
                return instance;
            }
        }
        // 如果没有找到合适的实例，返回null
        return null;
    }

    @Override
    public String mark() {
        return WEIGHT_LOAD_BALANCE_STRATEGY;
    }

}
