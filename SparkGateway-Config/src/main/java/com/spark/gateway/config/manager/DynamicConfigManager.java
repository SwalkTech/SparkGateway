package com.spark.gateway.config.manager;


import com.spark.gateway.config.pojo.RouteDefinition;
import com.spark.gateway.config.pojo.ServiceDefinition;
import com.spark.gateway.config.pojo.ServiceInstance;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 动态配置管理，缓存从配置中心拉取下来的配置
 */
public class DynamicConfigManager {

    private static final DynamicConfigManager INSTANCE = new DynamicConfigManager();
    // 路由规则变化监听器
    private final ConcurrentHashMap<String /* 服务名 */, List<RouteListener>> routeListenerMap = new ConcurrentHashMap<>();
    // 路由id对应的路由
    private final ConcurrentHashMap<String /* 路由id */, RouteDefinition> routeId2RouteMap = new ConcurrentHashMap<>();
    // 服务对应的路由
    private final ConcurrentHashMap<String /* 服务名 */, RouteDefinition> serviceName2RouteMap = new ConcurrentHashMap<>();
    // URI对应的路由
    private final ConcurrentHashMap<String /* URI路径 */, RouteDefinition> uri2RouteMap = new ConcurrentHashMap<>();
    // 服务
    private final ConcurrentHashMap<String /* 服务名 */, ServiceDefinition> serviceDefinitionMap = new ConcurrentHashMap<>();
    // 服务对应的实例
    private final ConcurrentHashMap<String /* 服务名 */, ConcurrentHashMap<String /* 实例id */, ServiceInstance>> serviceInstanceMap = new ConcurrentHashMap<>();

    /*********   单例   *********/
    private DynamicConfigManager() {
    }

    public static DynamicConfigManager getInstance() {
        return INSTANCE;
    }

    /*********   路由   *********/
    public void updateRouteByRouteId(String id, RouteDefinition routeDefinition) {
        routeId2RouteMap.put(id, routeDefinition);
    }

    public void updateRoutes(Collection<RouteDefinition> routes) {
        updateRoutes(routes, false);
    }

    /**
     * 更新路由信息
     * 此方法根据提供的路由定义集合更新内部路由映射
     * 如果指定清除旧路由，则在添加新路由之前清除所有现有路由映射
     *
     * @param routes 包含路由定义的集合，用于更新内部路由映射
     * @param clear  指示是否在添加新路由之前清除所有现有路由的布尔值
     */
    public void updateRoutes(Collection<RouteDefinition> routes, boolean clear) {
        // 检查提供的路由集合是否为空或为null，如果是，则不执行任何操作
        if (routes == null || routes.isEmpty()) {
            return;
        }
        // 如果指定清除旧路由，则清除所有现有路由映射
        if (clear) {
            routeId2RouteMap.clear();
            serviceName2RouteMap.clear();
            uri2RouteMap.clear();
        }
        // 遍历提供的路由集合，更新路由映射
        for (RouteDefinition route : routes) {
            // 如果当前路由定义为null，则跳过当前路由，继续处理下一个
            if (route == null) {
                continue;
            }
            // 将当前路由添加到基于路由ID的映射中
            routeId2RouteMap.put(route.getId(), route);
            // 将当前路由添加到基于服务名称的映射中
            serviceName2RouteMap.put(route.getServiceName(), route);
            // 将当前路由添加到基于URI的映射中
            uri2RouteMap.put(route.getUri(), route);
        }
    }

    public RouteDefinition getRouteById(String id) {
        return routeId2RouteMap.get(id);
    }

    public RouteDefinition getRouteByServiceName(String serviceName) {
        return serviceName2RouteMap.get(serviceName);
    }

    public Set<Map.Entry<String, RouteDefinition>> getAllUriEntry() {
        return uri2RouteMap.entrySet();
    }

    /*********   服务   *********/
    public void updateService(ServiceDefinition serviceDefinition) {
        serviceDefinitionMap.put(serviceDefinition.getServiceName(), serviceDefinition);
    }

    public ServiceDefinition getServiceByName(String name) {
        return serviceDefinitionMap.get(name);
    }

    /*********   实例   *********/
    public void addServiceInstance(String serviceName, ServiceInstance instance) {
        serviceInstanceMap.computeIfAbsent(serviceName, k -> new ConcurrentHashMap<>()).put(instance.getInstanceId(), instance);
    }

    public void updateInstances(ServiceDefinition serviceDefinition, Set<ServiceInstance> newInstances) {
        ConcurrentHashMap<String, ServiceInstance> oldInstancesMap = serviceInstanceMap.computeIfAbsent(serviceDefinition.getServiceName(), k -> new ConcurrentHashMap<>());
        oldInstancesMap.clear();
        for (ServiceInstance newInstance : newInstances) {
            oldInstancesMap.put(newInstance.getInstanceId(), newInstance);
        }
    }

    public void removeServiceInstance(String serviceName, ServiceInstance instance) {
        serviceInstanceMap.compute(serviceName, (k, v) -> {
            if (v == null || v.get(instance.getInstanceId()) == null) {
                return v;
            }
            v.remove(instance.getInstanceId());
            return v;
        });
    }

    public Map<String, ServiceInstance> getInstancesByServiceName(String serviceName) {
        return serviceInstanceMap.get(serviceName);
    }

    /*********   监听   *********/
    public void addRouteListener(String serviceName, RouteListener listener) {
        routeListenerMap.computeIfAbsent(serviceName, key -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void changeRoute(RouteDefinition routeDefinition) {
        List<RouteListener> routeListeners = routeListenerMap.get(routeDefinition.getServiceName());
        if (routeListeners == null || routeListeners.isEmpty()) {
            return;
        }
        for (RouteListener routeListener : routeListeners) {
            routeListener.changeOnRoute(routeDefinition);
        }
    }

}
