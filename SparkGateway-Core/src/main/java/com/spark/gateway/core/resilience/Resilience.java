package com.spark.gateway.core.resilience;


import com.spark.enums.ResilienceEnum;
import com.spark.enums.ResponseCode;
import com.spark.gateway.config.pojo.RouteDefinition;
import com.spark.gateway.core.context.GatewayContext;
import com.spark.gateway.core.filter.route.RouteUtil;
import com.spark.gateway.core.helper.ContextHelper;
import com.spark.gateway.core.helper.ResponseHelper;
import com.spark.gateway.core.resilience.fallback.FallbackHandler;
import com.spark.gateway.core.resilience.fallback.FallbackHandlerManager;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.asynchttpclient.Response;

import java.util.concurrent.*;
import java.util.function.Supplier;


public class Resilience {

    private static final Resilience INSTANCE = new Resilience();

    ScheduledExecutorService retryScheduler = Executors.newScheduledThreadPool(10);

    private Resilience() {
    }

    public static Resilience getInstance() {
        return INSTANCE;
    }

    /**
     * 执行请求并应用韧性配置
     * 该方法根据韧性配置对请求进行处理，以确保服务在面对故障时能够保持一定的稳定性
     *
     * @param gatewayContext 网关上下文，包含路由信息、请求信息等
     */
    public void executeRequest(GatewayContext gatewayContext) {
        // 获取韧性配置
        RouteDefinition.ResilienceConfig resilienceConfig = gatewayContext.getRoute().getResilience();
        // 获取服务名称
        String serviceName = gatewayContext.getRequest().getServiceDefinition().getServiceName();

        // 构建路由供应商函数
        Supplier<CompletionStage<Response>> supplier = RouteUtil.buildRouteSupplier(gatewayContext);

        // 遍历韧性配置的顺序，应用相应的韧性策略
        for (ResilienceEnum resilienceEnum : resilienceConfig.getOrder()) {
            switch (resilienceEnum) {
                case RETRY -> {
                    // 构建重试策略
                    Retry retry = ResilienceFactory.buildRetry(resilienceConfig, serviceName);
                    // 如果重试策略不为空，则装饰供应商函数
                    if (retry != null) {
                        supplier = Retry.decorateCompletionStage(retry, retryScheduler, supplier);
                    }
                }
                case FALLBACK -> {
                    // 如果启用了回退策略，则装饰供应商函数
                    if (resilienceConfig.isFallbackEnabled()) {
                        Supplier<CompletionStage<Response>> finalSupplier = supplier;
                        supplier = () ->
                                finalSupplier.get().exceptionally(throwable -> {
                                    // 获取并执行回退处理器
                                    FallbackHandler handler = FallbackHandlerManager.getHandler(resilienceConfig.getFallbackHandlerName());
                                    handler.handle(throwable, gatewayContext);
                                    return null;
                                });
                    }
                }
                case CIRCUITBREAKER -> {
                    // 构建断路器
                    CircuitBreaker circuitBreaker = ResilienceFactory.buildCircuitBreaker(resilienceConfig, serviceName);
                    // 如果断路器不为空，则装饰供应商函数
                    if (circuitBreaker != null) {
                        supplier = CircuitBreaker.decorateCompletionStage(circuitBreaker, supplier);
                    }
                }
                case BULKHEAD -> {
                    // 构建舱壁
                    Bulkhead bulkhead = ResilienceFactory.buildBulkHead(resilienceConfig, serviceName);
                    // 如果舱壁不为空，则装饰供应商函数
                    if (bulkhead != null) {
                        supplier = Bulkhead.decorateCompletionStage(bulkhead, supplier);
                    }
                }
                case THREADPOOLBULKHEAD -> {
                    // 构建线程池舱壁
                    ThreadPoolBulkhead threadPoolBulkhead = ResilienceFactory.buildThreadPoolBulkhead(resilienceConfig, serviceName);
                    // 如果线程池舱壁不为空，则装饰供应商函数
                    if (threadPoolBulkhead != null) {
                        Supplier<CompletionStage<Response>> finalSupplier = supplier;
                        supplier = () -> {
                            CompletionStage<CompletableFuture<Response>> future =
                                    threadPoolBulkhead.executeSupplier(() -> finalSupplier.get().toCompletableFuture());
                            try {
                                return future.toCompletableFuture().get();
                            } catch (InterruptedException | ExecutionException e) {
                                throw new RuntimeException(e);
                            }
                        };
                    }
                }
            }
        }

        // 处理执行过程中可能抛出的异常
        supplier.get().exceptionally(throwable -> {
            // 如果未启用回退策略，则设置异常信息并构建错误响应
            if (!resilienceConfig.isFallbackEnabled()) {
                gatewayContext.setThrowable(throwable);
                gatewayContext.setResponse(ResponseHelper.buildGatewayResponse(ResponseCode.SERVICE_UNAVAILABLE));
                ContextHelper.writeBackResponse(gatewayContext);
            }
            return null;
        });
    }

}
