package com.spark.gateway.core.filter.loadbalance.strategy;

import com.spark.gateway.config.pojo.ServiceInstance;
import com.spark.gateway.core.context.GatewayContext;

import java.util.List;

import static com.spark.constant.LoadBalanceConstant.GRAY_LOAD_BALANCE_STRATEGY;


public class GrayLoadBalanceStrategy implements LoadBalanceStrategy {

    /**
     * 根据请求选择一个服务实例
     * 该方法使用轮询算法基于服务实例的阈值来选择一个实例
     * 如果总阈值为零或负数，则不选择任何实例
     *
     * @param context   网关上下文，包含请求信息
     * @param instances 服务实例列表，不为空
     * @return 选定的服务实例，如果没有合适的实例则返回null
     */
    @Override
    public ServiceInstance selectInstance(GatewayContext context, List<ServiceInstance> instances) {
        // 计算所有实例阈值的总和，转换为百分比形式
        int totalThreshold = (int) (instances.stream().mapToDouble(ServiceInstance::getThreshold).sum() * 100);
        // 如果总阈值不大于0，则不选择任何实例
        if (totalThreshold <= 0) {
            return null;
        }
        // 根据请求主机的哈希码和总阈值计算一个随机阈值，以实现负载均衡
        int randomThreshold = Math.abs(context.getRequest().getHost().hashCode()) % totalThreshold;
        // 遍历所有实例，逐个减去其阈值，直到随机阈值小于0，即找到目标实例
        for (ServiceInstance instance : instances) {
            randomThreshold -= instance.getThreshold();
            // 当随机阈值小于0时，返回当前实例
            if (randomThreshold < 0) {
                return instance;
            }
        }
        // 如果所有实例遍历完毕仍未找到目标实例，返回null
        return null;
    }

    @Override
    public String mark() {
        return GRAY_LOAD_BALANCE_STRATEGY;
    }

}
