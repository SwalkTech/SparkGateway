package com.spark.gateway.core.filter.flow;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.spark.gateway.config.manager.DynamicConfigManager;
import com.spark.gateway.config.pojo.RouteDefinition;
import com.spark.gateway.config.util.FilterUtil;
import com.spark.gateway.core.algorithm.LeakyBucketRateLimiter;
import com.spark.gateway.core.algorithm.SlidingWindowRateLimiter;
import com.spark.gateway.core.algorithm.TokenBucketRateLimiter;
import com.spark.gateway.core.context.GatewayContext;
import com.spark.gateway.core.filter.Filter;
import io.netty.channel.EventLoop;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.spark.constant.FilterConstant.FLOW_FILTER_NAME;
import static com.spark.constant.FilterConstant.FLOW_FILTER_ORDER;


/**
 * @author: spark
 * @date: 2024/12/30 10:17
 * @description: 流量过滤器
 **/
public class FlowFilter implements Filter {

    private final ConcurrentHashMap<String /* 服务名 */, RateLimiter> rateLimiterMap = new ConcurrentHashMap<>();

    private final Set<String> addListener = new ConcurrentHashSet<>();

    /**
     * 执行预过滤操作
     * 本方法主要用于流控过滤，根据路由配置中的流控设置决定是否执行流控检查
     * 如果未开启流控或流控配置不存在，则直接进行下一个过滤操作
     * 如果开启了流控，则根据服务名初始化或获取已有的RateLimiter对象进行流控检查
     *
     * @param context 网关上下文，包含路由、请求等信息
     */
    @Override
    public void doPreFilter(GatewayContext context) {

        // 查找并解析流控过滤配置，如果不存在则创建一个新的FlowFilterConfig对象
        RouteDefinition.FlowFilterConfig flowFilterConfig = Optional
                .ofNullable(FilterUtil.findFilterConfigByClass(context.getRoute().getFilterConfigs(), FLOW_FILTER_NAME, RouteDefinition.FlowFilterConfig.class))
                .orElse(new RouteDefinition.FlowFilterConfig());

        // 如果流控功能未启用，直接进行下一个过滤操作
        if (!flowFilterConfig.isEnabled()) {
            context.doFilter();
        } else {
            // 获取当前请求的服务名
            String serviceName = context.getRequest().getServiceDefinition().getServiceName();
            // 使用服务名作为键，从rateLimiterMap中获取或初始化RateLimiter对象
            RateLimiter rateLimiter = rateLimiterMap.computeIfAbsent(serviceName, name -> {
                // 如果当前服务名未添加监听器，则添加
                if (!addListener.contains(name)) {
                    DynamicConfigManager.getInstance().addRouteListener(name, newRoute -> {
                        // 当路由更新时，移除旧的RateLimiter
                        rateLimiterMap.remove(newRoute.getServiceName());
                    });
                    addListener.add(name);
                }
                // 初始化并返回RateLimiter对象
                return initRateLimiter(flowFilterConfig, context.getNettyCtx().channel().eventLoop());
            });
            // 使用RateLimiter尝试消费，即执行流控检查
            rateLimiter.tryConsume(context);
        }
    }

    @Override
    public void doPostFilter(GatewayContext context) {
        context.doFilter();
    }

    @Override
    public String mark() {
        return FLOW_FILTER_NAME;
    }

    @Override
    public int getOrder() {
        return FLOW_FILTER_ORDER;
    }

    /**
     * 初始化速率限制器
     * <p>
     * 根据路由定义中的流量过滤配置和事件循环来选择合适的速率限制算法实现
     * 此方法使用了@ SuppressWarnings("DuplicateBranchesInSwitch")注解以忽略switch语句中的重复分支警告
     *
     * @param flowFilterConfig 流量过滤配置，包含速率限制的类型、容量和速率
     * @param eventLoop        事件循环，用于处理泄漏桶速率限制算法中的异步任务
     * @return 根据配置创建的速率限制器实例
     */
    @SuppressWarnings("DuplicateBranchesInSwitch")
    private RateLimiter initRateLimiter(RouteDefinition.FlowFilterConfig flowFilterConfig, EventLoop eventLoop) {
        // 根据流量过滤配置的类型选择速率限制算法
        switch (flowFilterConfig.getType()) {
            case TOKEN_BUCKET -> {
                // 创建并返回令牌桶速率限制器
                return new TokenBucketRateLimiter(flowFilterConfig.getCapacity(), flowFilterConfig.getRate());
            }
            case SLIDING_WINDOW -> {
                // 创建并返回滑动窗口速率限制器
                return new SlidingWindowRateLimiter(flowFilterConfig.getCapacity(), flowFilterConfig.getRate());
            }
            case LEAKY_BUCKET -> {
                // 创建并返回泄漏桶速率限制器
                return new LeakyBucketRateLimiter(flowFilterConfig.getCapacity(), flowFilterConfig.getRate(), eventLoop);
            }
            default -> {
                // 默认情况下创建并返回令牌桶速率限制器
                return new TokenBucketRateLimiter(flowFilterConfig.getCapacity(), flowFilterConfig.getRate());
            }
        }
    }

}
