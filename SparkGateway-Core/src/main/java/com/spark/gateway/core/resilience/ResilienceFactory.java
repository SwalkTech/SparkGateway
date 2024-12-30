package com.spark.gateway.core.resilience;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.spark.enums.CircuitBreakerEnum;
import com.spark.gateway.config.manager.DynamicConfigManager;
import com.spark.gateway.config.pojo.RouteDefinition;
import io.github.resilience4j.bulkhead.*;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * ResilienceFactory类用于根据服务名称和韧性配置，构建不同的韧性策略实例
 * 包括重试、断路器、舱壁和线程池舱壁策略
 */
public class ResilienceFactory {

    // 重试策略映射，用于缓存不同服务的重试策略实例
    private static final Map<String, Retry> retryMap = new ConcurrentHashMap<>();
    // 断路器策略映射，用于缓存不同服务的断路器策略实例
    private static final Map<String, CircuitBreaker> circuitBreakerMap = new ConcurrentHashMap<>();
    // 舱壁策略映射，用于缓存不同服务的舱壁策略实例
    private static final Map<String, Bulkhead> bulkheadMap = new ConcurrentHashMap<>();
    // 线程池舱壁策略映射，用于缓存不同服务的线程池舱壁策略实例
    private static final Map<String, ThreadPoolBulkhead> threadPoolBulkheadMap = new ConcurrentHashMap<>();

    // 重试策略名称集合，用于记录已经创建的重试策略
    private static final Set<String> retrySet = new ConcurrentHashSet<>();
    // 断路器策略名称集合，用于记录已经创建的断路器策略
    private static final Set<String> circuitBreakerSet = new ConcurrentHashSet<>();
    // 舱壁策略名称集合，用于记录已经创建的舱壁策略
    private static final Set<String> bulkheadSet = new ConcurrentHashSet<>();
    // 线程池舱壁策略名称集合，用于记录已经创建的线程池舱壁策略
    private static final Set<String> threadPoolBulkheadSet = new ConcurrentHashSet<>();

    /**
     * 根据韧性和服务名称构建重试策略实例
     * 如果重试功能未启用，则返回null
     * 如果已经存在该服务的重试策略，则直接返回缓存的实例
     * 否则，根据配置创建新的重试策略实例，并缓存该实例
     *
     * @param resilienceConfig 韧性配置，包含重试相关的配置信息
     * @param serviceName      服务名称，用于标识和缓存对应的重试策略
     * @return 返回构建的重试策略实例，如果重试功能未启用则返回null
     */
    public static Retry buildRetry(RouteDefinition.ResilienceConfig resilienceConfig, String serviceName) {
        // 检查重试功能是否已启用
        if (!resilienceConfig.isRetryEnabled()) {
            return null;
        }
        // 尝试从缓存中获取已存在的重试策略实例
        return retryMap.computeIfAbsent(serviceName, name -> {
            // 检查是否已经订阅了路由监听器
            if (!retrySet.contains(serviceName)) {
                // 添加路由监听器，以便在路由变更时清除缓存
                DynamicConfigManager.getInstance().addRouteListener(serviceName, newRoute -> retryMap.remove(newRoute.getServiceName()));
                retrySet.add(serviceName);
            }
            // 构建重试配置
            RetryConfig config = RetryConfig.custom()
                    .maxAttempts(resilienceConfig.getMaxAttempts())
                    .waitDuration(Duration.ofMillis(resilienceConfig.getWaitDuration()))
                    .build();
            // 根据配置创建并返回重试策略实例
            return RetryRegistry.of(config).retry(serviceName);
        });
    }

    /**
     * 根据韧性和服务名称构建断路器策略实例
     * 如果断路器功能未启用，则返回null
     * 如果已经存在该服务的断路器策略，则直接返回缓存的实例
     * 否则，根据配置创建新的断路器策略实例，并缓存该实例
     *
     * @param resilienceConfig 韧性配置，包含断路器相关的配置信息
     * @param serviceName      服务名称，用于标识和缓存对应的断路器策略
     * @return 返回构建的断路器策略实例，如果断路器功能未启用则返回null
     */
    public static CircuitBreaker buildCircuitBreaker(RouteDefinition.ResilienceConfig resilienceConfig, String serviceName) {
        // 检查断路器功能是否启用
        if (!resilienceConfig.isCircuitBreakerEnabled()) {
            return null;
        }
        // 使用computeIfAbsent确保线程安全地获取或创建断路器实例
        return circuitBreakerMap.computeIfAbsent(serviceName, name -> {
            // 确保每个服务只添加一次路由监听器，避免重复订阅
            if (!circuitBreakerSet.contains(serviceName)) {
                DynamicConfigManager.getInstance().addRouteListener(serviceName, newRoute -> circuitBreakerMap.remove(newRoute.getServiceName()));
                circuitBreakerSet.add(serviceName);
            }
            // 根据韧性配置构建断路器配置
            CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                    .failureRateThreshold(resilienceConfig.getFailureRateThreshold())
                    .slowCallRateThreshold(resilienceConfig.getSlowCallRateThreshold())
                    .waitDurationInOpenState(Duration.ofMillis(resilienceConfig.getWaitDurationInOpenState()))
                    .slowCallDurationThreshold(Duration.ofSeconds(resilienceConfig.getSlowCallDurationThreshold()))
                    .permittedNumberOfCallsInHalfOpenState(resilienceConfig.getPermittedNumberOfCallsInHalfOpenState())
                    .minimumNumberOfCalls(resilienceConfig.getMinimumNumberOfCalls())
                    .slidingWindowType(slidingWindowTypeConvert(resilienceConfig.getType()))
                    .slidingWindowSize(resilienceConfig.getSlidingWindowSize())
                    .build();
            // 创建并返回断路器实例
            return CircuitBreakerRegistry.of(circuitBreakerConfig).circuitBreaker(serviceName);
        });
    }

    /**
     * 根据韧性和服务名称构建舱壁策略实例
     * 如果舱壁功能未启用，则返回null
     * 如果已经存在该服务的舱壁策略，则直接返回缓存的实例
     * 否则，根据配置创建新的舱壁策略实例，并缓存该实例
     *
     * @param resilienceConfig 韧性配置，包含舱壁相关的配置信息
     * @param serviceName      服务名称，用于标识和缓存对应的舱壁策略
     * @return 返回构建的舱壁策略实例，如果舱壁功能未启用则返回null
     */
    public static Bulkhead buildBulkHead(RouteDefinition.ResilienceConfig resilienceConfig, String serviceName) {
        // 检查舱壁功能是否启用，如果未启用则直接返回null
        if (!resilienceConfig.isBulkheadEnabled()) {
            return null;
        }
        // 尝试从缓存中获取已存在的舱壁策略实例，如果不存在则创建并缓存
        return bulkheadMap.computeIfAbsent(serviceName, name -> {
            // 检查是否已经订阅了路由监听，如果没有则订阅
            if (!bulkheadSet.contains(serviceName)) {
                DynamicConfigManager.getInstance().addRouteListener(serviceName, newRoute -> bulkheadMap.remove(newRoute.getServiceName()));
                bulkheadSet.add(serviceName);
            }
            // 根据韧性配置构建舱壁策略配置
            BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
                    .maxConcurrentCalls(resilienceConfig.getMaxConcurrentCalls())
                    .maxWaitDuration(Duration.ofMillis(resilienceConfig.getMaxWaitDuration()))
                    .fairCallHandlingStrategyEnabled(resilienceConfig.isFairCallHandlingEnabled()).build();
            // 根据构建的配置创建并返回舱壁策略实例
            return BulkheadRegistry.of(bulkheadConfig).bulkhead(serviceName);
        });
    }

    /**
     * 根据韧性和服务名称构建线程池舱壁策略实例
     * 如果线程池舱壁功能未启用，则返回null
     * 如果已经存在该服务的线程池舱壁策略，则直接返回缓存的实例
     * 否则，根据配置创建新的线程池舱壁策略实例，并缓存该实例
     *
     * @param resilienceConfig 韧性配置，包含线程池舱壁相关的配置信息
     * @param serviceName      服务名称，用于标识和缓存对应的线程池舱壁策略
     * @return 返回构建的线程池舱壁策略实例，如果线程池舱壁功能未启用则返回null
     */
    public static ThreadPoolBulkhead buildThreadPoolBulkhead(RouteDefinition.ResilienceConfig resilienceConfig, String serviceName) {
        // 检查线程池舱壁功能是否启用，未启用则直接返回null
        if (!resilienceConfig.isThreadPoolBulkheadEnabled()) {
            return null;
        }
        // 使用computeIfAbsent确保线程安全地检查和创建线程池舱壁策略实例
        return threadPoolBulkheadMap.computeIfAbsent(serviceName, name -> {
            // 确保每个服务只监听一次，避免重复监听
            if (!threadPoolBulkheadSet.contains(serviceName)) {
                // 添加路由监听器，当路由更新时移除对应的线程池舱壁策略
                DynamicConfigManager.getInstance().addRouteListener(serviceName, newRoute -> threadPoolBulkheadMap.remove(newRoute.getServiceName()));
                threadPoolBulkheadSet.add(serviceName);
            }
            // 根据韧性配置创建线程池舱壁策略配置
            ThreadPoolBulkheadConfig threadPoolBulkheadConfig = ThreadPoolBulkheadConfig.custom()
                    .coreThreadPoolSize(resilienceConfig.getCoreThreadPoolSize())
                    .maxThreadPoolSize(resilienceConfig.getMaxThreadPoolSize())
                    .queueCapacity(resilienceConfig.getQueueCapacity())
                    .build();
            // 根据配置创建并返回线程池舱壁策略实例
            return ThreadPoolBulkheadRegistry.of(threadPoolBulkheadConfig).bulkhead(serviceName);
        });
    }

    /**
     * 将CircuitBreakerEnum类型的滑动窗口类型转换为CircuitBreakerConfig.SlidingWindowType类型
     * 如果输入类型为TIME_BASED，则返回CircuitBreakerConfig.SlidingWindowType.TIME_BASED
     * 否则返回CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
     *
     * @param from 需要转换的CircuitBreakerEnum类型
     * @return 转换后的CircuitBreakerConfig.SlidingWindowType类型
     */
    private static CircuitBreakerConfig.SlidingWindowType slidingWindowTypeConvert(CircuitBreakerEnum from) {
        if (from == CircuitBreakerEnum.TIME_BASED) {
            return CircuitBreakerConfig.SlidingWindowType.TIME_BASED;
        } else {
            return CircuitBreakerConfig.SlidingWindowType.COUNT_BASED;
        }
    }

}
