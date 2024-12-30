package com.spark.gateway.core.filter.loadbalance.strategy;

import com.spark.gateway.config.pojo.RouteDefinition;
import com.spark.gateway.config.pojo.ServiceInstance;
import com.spark.gateway.config.util.FilterUtil;
import com.spark.gateway.core.algorithm.ConsistentHashing;
import com.spark.gateway.core.context.GatewayContext;

import java.util.List;

import static com.spark.constant.FilterConstant.LOAD_BALANCE_FILTER_NAME;
import static com.spark.constant.LoadBalanceConstant.CLIENT_IP_CONSISTENT_HASH_LOAD_BALANCE_STRATEGY;


public class ClientIpConsistentHashLoadBalanceStrategy implements LoadBalanceStrategy {

    /**
     * 选择实例方法，基于一致性哈希算法进行负载均衡
     *
     * @param context   网关上下文，包含路由和请求等信息
     * @param instances 服务实例列表，用于选择合适的实例
     * @return 返回选中的服务实例
     */
    @Override
    public ServiceInstance selectInstance(GatewayContext context, List<ServiceInstance> instances) {
        // 获取路由定义中的一致性哈希过滤器配置
        RouteDefinition.LoadBalanceFilterConfig loadBalanceFilterConfig = FilterUtil.findFilterConfigByClass(context.getRoute().getFilterConfigs(), LOAD_BALANCE_FILTER_NAME, RouteDefinition.LoadBalanceFilterConfig.class);
        // 默认虚拟节点数量为1
        int virtualNodeNum = 1;
        // 如果配置存在且配置了虚拟节点数量，则使用配置的值
        if (loadBalanceFilterConfig != null && loadBalanceFilterConfig.getVirtualNodeNum() > 0) {
            virtualNodeNum = loadBalanceFilterConfig.getVirtualNodeNum();
        }

        // 将实例列表转换为实例ID列表，用于一致性哈希算法
        List<String> nodes = instances.stream().map(ServiceInstance::getInstanceId).toList();
        // 创建一致性哈希对象，传入实例ID列表和虚拟节点数量
        ConsistentHashing consistentHashing = new ConsistentHashing(nodes, virtualNodeNum);
        // 根据请求的主机哈希值选择节点
        String selectedNode = consistentHashing.getNode(String.valueOf(context.getRequest().getHost().hashCode()));

        // 遍历实例列表，寻找与选中节点匹配的实例
        for (ServiceInstance instance : instances) {
            if (instance.getInstanceId().equals(selectedNode)) {
                // 如果找到匹配的实例，返回该实例
                return instance;
            }
        }

        // 如果没有找到匹配的实例，返回第一个实例作为默认选择
        return instances.get(0);
    }

    @Override
    public String mark() {
        return CLIENT_IP_CONSISTENT_HASH_LOAD_BALANCE_STRATEGY;
    }

}
