package com.spark.gateway.config.service.impl.nacos;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spark.gateway.config.config.ConfigCenter;
import com.spark.gateway.config.config.lib.nacos.NacosConfig;
import com.spark.gateway.config.pojo.RouteDefinition;
import com.spark.gateway.config.service.ConfigCenterProcessor;
import com.spark.gateway.config.service.RoutesChangeListener;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
public class NacosConfigCenter implements ConfigCenterProcessor {

    /**
     * 配置
     */
    private ConfigCenter configCenter;

    /**
     * Nacos提供的与配置中心进行交互的接口
     */
    private ConfigService configService;

    /**
     * 是否完成初始化
     */
    private final AtomicBoolean init = new AtomicBoolean(false);

    @Override
    @SneakyThrows(NacosException.class)
    public void init(ConfigCenter configCenter) {
        if (!configCenter.isEnabled() || !init.compareAndSet(false, true)) {
            return;
        }
        this.configCenter = configCenter;
        this.configService = NacosFactory.createConfigService(buildProperties(configCenter));
    }

    /**
     * 订阅路由变更事件
     * 当路由配置发生变化时，通知监听器
     *
     * @param listener 路由变更事件监听器，用于处理路由配置变化后的逻辑
     */
    @Override
    @SneakyThrows(NacosException.class)
    public void subscribeRoutesChange(RoutesChangeListener listener) {
        // 检查配置中心是否已启用且初始化成功，若未满足条件则直接返回
        if (!configCenter.isEnabled() || !init.get()) {
            return;
        }

        // 获取Nacos配置信息
        NacosConfig nacos = configCenter.getNacos();
        // 从Nacos中获取路由配置信息
        String configJson = configService.getConfig(nacos.getDataId(), nacos.getGroup(), nacos.getTimeout());
        /*
         * configJson示例:
         * {
         *     "routes": [
         *         {
         *             "id": "user-service-route",
         *             "serviceName": "user-service",
         *             "uri": "/api/user/**"
         *         }
         *     ]
         * }
         */
        // 日志输出获取到的配置信息
        log.info("config from nacos: \n{}", configJson);

        // 解析配置信息中的路由定义，并通知监听器
        List<RouteDefinition> routes = JSON.parseObject(configJson).getJSONArray("routes").toJavaList(RouteDefinition.class);
        listener.onRoutesChange(routes);

        // 为Nacos配置添加监听器，以便在配置变更时收到通知
        configService.addListener(nacos.getDataId(), nacos.getGroup(), new Listener() {
            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                // 当配置信息变更时，日志输出变更内容
                log.info("config change from nacos: {}", configInfo);
                // 解析变更后的配置信息中的路由定义，并通知监听器
                List<RouteDefinition> routes = JSON.parseObject(configInfo).getJSONArray("routes").toJavaList(RouteDefinition.class);
                listener.onRoutesChange(routes);
            }
        });
    }

    /**
     * 根据配置中心的信息构建Properties对象
     * 此方法主要用于将配置中心的相关信息转换为Properties对象，以便在应用程序中使用
     *
     * @param configCenter 配置中心对象，包含配置中心的地址和Nacos配置等信息
     * @return Properties对象，包含了配置中心的地址和Nacos配置等信息
     */
    private Properties buildProperties(ConfigCenter configCenter) {
        // 创建ObjectMapper对象，用于转换配置中心的Nacos配置
        ObjectMapper mapper = new ObjectMapper();
        // 创建Properties对象，用于存储配置信息
        Properties properties = new Properties();

        // 将配置中心的地址放入Properties对象中
        properties.put(PropertyKeyConst.SERVER_ADDR, configCenter.getAddress());

        // 将配置中心的Nacos配置转换为Map对象
        Map map = mapper.convertValue(configCenter.getNacos(), Map.class);
        
        // 如果Nacos配置为空，则直接返回当前的Properties对象
        if (map == null || map.isEmpty()) {
            return properties;
        }

        // 将Nacos配置的Map对象中的所有键值对添加到Properties对象中
        properties.putAll(map);

        // 返回包含配置信息的Properties对象
        return properties;
    }

}
