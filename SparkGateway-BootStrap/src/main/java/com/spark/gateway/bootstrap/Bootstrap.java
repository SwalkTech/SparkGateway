package com.spark.gateway.bootstrap;

import com.spark.gateway.config.config.Config;
import com.spark.gateway.config.loader.ConfigLoader;
import com.spark.gateway.config.manager.DynamicConfigManager;
import com.spark.gateway.config.service.ConfigCenterProcessor;
import com.spark.gateway.core.config.Container;
import com.spark.service.RegisterCenterProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.ServiceLoader;


@Slf4j
public class Bootstrap {

    private Config config;

    private Container container;

    public static void run(String[] args) {
        new Bootstrap().start(args);
    }

    public void start(String[] args) {
        log.info("gateway bootstrap start...");

        // 加载配置
        config = ConfigLoader.load(args);
        log.info("gateway bootstrap load config: {}", config);

        // 初始化配置中心
        initConfigCenter();

        // 启动容器
        initContainer();
        container.start();

        // 初始化注册中心
        initRegisterCenter();

        // 注册钩子，优雅停机
        registerGracefullyShutdown();
    }

    private void initConfigCenter() {
        try {
            ConfigCenterProcessor configCenterProcessor = ServiceLoader.load(ConfigCenterProcessor.class)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("未能找到 ConfigCenter 实现类", new Exception()));

            configCenterProcessor.init(config.getConfigCenter());

            configCenterProcessor.subscribeRoutesChange(newRoutes -> {
                if (newRoutes == null || newRoutes.isEmpty()) {
                    log.warn("接收到的路由配置为空");
                    return;
                }

                synchronized (DynamicConfigManager.class) {
                    DynamicConfigManager.getInstance().updateRoutes(newRoutes, true);
                    // 如果 updateRoutes 已经包含了 changeRoute 的逻辑，则无需再单独调用 changeRoute
                    // for (RouteDefinition newRoute : newRoutes) {
                    //     DynamicConfigManager.getInstance().changeRoute(newRoute);
                    // }
                }
            });
        } catch (Exception e) {
            log.error("初始化配置中心时发生错误", e);
            throw new RuntimeException("初始化配置中心失败", e);
        }
    }


    private void initContainer() {
        container = new Container(config);
    }

    private void initRegisterCenter() {
        RegisterCenterProcessor registerCenterProcessor = ServiceLoader.load(RegisterCenterProcessor.class).findFirst().orElseThrow(() -> {
            log.error("not found RegisterCenter impl");
            return new RuntimeException("not found RegisterCenter impl");
        });
        registerCenterProcessor.init(config);
        registerCenterProcessor.subscribeServiceChange(((serviceDefinition, newInstances) -> {
            DynamicConfigManager.getInstance().updateService(serviceDefinition);
            DynamicConfigManager.getInstance().updateInstances(serviceDefinition, newInstances);
        }));
    }

    private void registerGracefullyShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            container.shutdown();
        }));
    }

}
