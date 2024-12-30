package com.spark.gateway.core.filter.gray;

import cn.hutool.json.JSONUtil;
import com.spark.gateway.config.manager.DynamicConfigManager;
import com.spark.gateway.config.pojo.RouteDefinition;
import com.spark.gateway.config.pojo.ServiceInstance;
import com.spark.gateway.config.util.FilterUtil;
import com.spark.gateway.core.context.GatewayContext;
import com.spark.gateway.core.filter.Filter;
import com.spark.gateway.core.filter.gray.strategy.SparkStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.spark.constant.FilterConstant.SPARK_FILTER_NAME;
import static com.spark.constant.FilterConstant.SPARK_FILTER_ORDER;


@Slf4j
public class GrayFilter implements Filter {


    @Override
    public void doPreFilter(GatewayContext context) {
        // 根据过滤器名称查找路由定义中的过滤器配置
        RouteDefinition.FilterConfig filterConfig = FilterUtil.findFilterConfigByName(context.getRoute().getFilterConfigs(), SPARK_FILTER_NAME);
        // 如果未找到过滤器配置，则使用默认的过滤器配置
        if (filterConfig == null) {
            filterConfig = FilterUtil.buildDefaultGrayFilterConfig();
        }
        // 如果过滤器配置未启用，则直接返回，不进行后续处理
        if (!filterConfig.isEnable()) {
            return;
        }

        // 获取服务所有实例
        List<ServiceInstance> instances = DynamicConfigManager.getInstance()
                .getInstancesByServiceName(context.getRequest().getServiceDefinition().getServiceName())
                .values().stream().toList();

        // 检查是否存在启用且标记为灰度的实例
        if (instances.stream().anyMatch(instance -> instance.isEnabled() && instance.isGray())) {
            // 存在灰度实例
            // 根据过滤器配置选择灰度策略
            SparkStrategy strategy = selectGrayStrategy(JSONUtil.toBean(filterConfig.getConfig(), RouteDefinition.GrayFilterConfig.class));
            // 根据灰度策略判断是否路由到灰度实例
            context.getRequest().setGray(strategy.shouldRoute2Gray(context, instances));
        } else {
            // 灰度实例都没，不走灰度
            context.getRequest().setGray(false);
        }
        // 执行下一个过滤器
        context.doFilter();
    }

    @Override
    public void doPostFilter(GatewayContext context) {
        context.doFilter();
    }

    @Override
    public String mark() {
        return SPARK_FILTER_NAME;
    }

    @Override
    public int getOrder() {
        return SPARK_FILTER_ORDER;
    }

    /**
     * 根据灰度过滤配置选择灰度策略
     *
     * @param grayFilterConfig 灰度过滤配置，包含了策略名称等信息
     * @return 返回对应的灰度策略实例
     */
    private SparkStrategy selectGrayStrategy(RouteDefinition.GrayFilterConfig grayFilterConfig) {
        // 通过灰度策略名称获取对应的灰度策略实例
        return GrayStrategyManager.getStrategy(grayFilterConfig.getStrategyName());
    }

}
