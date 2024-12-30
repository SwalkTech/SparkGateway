package com.spark.service.impl.nacos;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingMaintainFactory;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.Service;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.common.executor.NameThreadFactory;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spark.gateway.config.config.Config;
import com.spark.gateway.config.config.RegisterCenter;
import com.spark.gateway.config.pojo.ServiceDefinition;
import com.spark.gateway.config.pojo.ServiceInstance;
import com.spark.service.RegisterCenterListener;
import com.spark.service.RegisterCenterProcessor;
import com.spark.util.NetUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


/**
 * @author: spark
 * @date: 2024/12/30 08:47
 * @description: Nacos注册中心实现类
 **/
@Slf4j
public class NacosRegisterCenter implements RegisterCenterProcessor {

    /**
     * 注册中心配置
     */
    private Config config;

    /**
     * 主要用于维护服务实例信息
     */
    private NamingService namingService;

    /**
     * 主要用于维护服务定义信息
     */
    private NamingMaintainService namingMaintainService;


    /**
     * 监听器
     */
    private RegisterCenterListener listener;

    /**
     * 初始化标记，确保只初始化一次
     */
    private final AtomicBoolean init = new AtomicBoolean(false);

    /**
     * 初始化注册中心
     *
     * @param config 注册中心配置
     */
    @SneakyThrows(Exception.class)
    @Override
    public void init(Config config) {
        // 双重检查锁定，确保只初始化一次
        if (!init.compareAndSet(false, true)) {
            return;
        }
        this.config = config;

        // 获取Nacos分组信息
        String group = config.getRegisterCenter().getNacos().getGroup();

        // 构建Nacos属性配置
        Properties properties = buildProperties(config.getRegisterCenter());
        // 创建命名服务和维护服务实例
        namingService = NamingFactory.createNamingService(properties);
        namingMaintainService = NamingMaintainFactory.createMaintainService(properties);

        // 将网关自己注册到注册中心
        Instance instance = new Instance();
        instance.setInstanceId(NetUtil.getLocalIp() + ":" + config.getPort());
        instance.setIp(NetUtil.getLocalIp());
        instance.setPort(config.getPort());
        namingService.registerInstance(config.getName(), group, instance);
        log.info("gateway instance register: {}", instance);

        // 设置网关服务元数据信息
        Map<String, String> serviceInfo = BeanUtils.describe(new ServiceDefinition(config.getName()));
        log.info("gateway service meta register: {}", serviceInfo);
        namingMaintainService.updateService(config.getName(), group, 0, serviceInfo);

    }

    /**
     * 订阅服务变更
     *
     * @param listener 服务变更监听器
     */
    @Override
    public void subscribeServiceChange(RegisterCenterListener listener) {
        this.listener = listener;

        // 使用定时任务定期执行服务订阅
        Executors.newScheduledThreadPool(1, new NameThreadFactory("doSubscribeAllServices")).
                scheduleWithFixedDelay(this::doSubscribeAllServices, 0, 10, TimeUnit.SECONDS);
    }

    /**
     * 构建Nacos属性配置
     *
     * @param registerCenter 注册中心配置
     * @return Nacos属性配置
     */
    private Properties buildProperties(RegisterCenter registerCenter) {
        ObjectMapper mapper = new ObjectMapper();
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, registerCenter.getAddress());
        Map map = mapper.convertValue(registerCenter.getNacos(), Map.class);
        if (map == null || map.isEmpty()) {
            return properties;
        }
        properties.putAll(map);
        return properties;
    }

    /**
     * 执行订阅所有服务
     */
    private void doSubscribeAllServices() {
        try {
            String group = config.getRegisterCenter().getNacos().getGroup();

            // 获取已订阅的服务集合
            Set<String> subscribeServiceSet = namingService.getSubscribeServices().stream().map(ServiceInfo::getName).collect(Collectors.toSet());

            int pageNo = 1;
            int pageSize = 100;

            // 分页获取服务列表并订阅
            List<String> serviceList = namingService.getServicesOfServer(pageNo, pageSize, group).getData();

            while (CollectionUtils.isNotEmpty(serviceList)) {
                for (String serviceName : serviceList) {
                    if (subscribeServiceSet.contains(serviceName)) {
                        continue;
                    }

                    EventListener eventListener = new NacosRegisterListener();
                    eventListener.onEvent(new NamingEvent(serviceName, null)); // 首次订阅新服务，主动发起一次信号
                    namingService.subscribe(serviceName, group, eventListener);
                    log.info("subscribe a service, ServiceName: {} Group: {}", serviceName, group);
                }
                //遍历下一页的服务列表
                serviceList = namingService.getServicesOfServer(++pageNo, pageSize, group).getData();
            }
        } catch (Exception e) { // 任务中捕捉Exception，防止线程池停止
            log.error("subscribe services from nacos occur exception: {}", e.getMessage(), e);
        }
    }

    /**
     * Nacos注册监听器，处理服务事件
     */
    private class NacosRegisterListener implements EventListener {

        @SneakyThrows(NacosException.class)
        @Override
        public void onEvent(Event event) {
            if (event instanceof NamingEvent namingEvent) {
                String serviceName = namingEvent.getServiceName();
                String group = config.getRegisterCenter().getNacos().getGroup();

                // 查询服务定义信息并解析元数据
                Service service = namingMaintainService.queryService(serviceName, group);
                ServiceDefinition serviceDefinition = new ServiceDefinition(service.getName());
                BeanUtil.fillBeanWithMap(service.getMetadata(), serviceDefinition, true);

                //获取所有服务实例信息
                List<Instance> allInstances = namingService.getAllInstances(serviceName, group);
                Set<ServiceInstance> newInstances = new HashSet<>();

                if (CollectionUtils.isNotEmpty(allInstances)) {
                    for (Instance instance : allInstances) {
                        if (instance == null) {
                            continue;
                        }

                        ServiceInstance newInstance = new ServiceInstance();
                        BeanUtil.copyProperties(instance, newInstance);
                        BeanUtil.fillBeanWithMap(instance.getMetadata(), newInstance, true);

                        newInstances.add(newInstance);
                    }
                }

                //调用我们自己的订阅监听器
                listener.onInstancesChange(serviceDefinition, newInstances);
            }
        }

    }
}
