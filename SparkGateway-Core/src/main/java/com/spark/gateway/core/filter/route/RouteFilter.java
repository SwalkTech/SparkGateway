package com.spark.gateway.core.filter.route;

import com.spark.enums.ResponseCode;
import com.spark.gateway.config.pojo.RouteDefinition;
import com.spark.gateway.core.context.GatewayContext;
import com.spark.gateway.core.filter.Filter;
import com.spark.gateway.core.helper.ContextHelper;
import com.spark.gateway.core.helper.ResponseHelper;
import com.spark.gateway.core.resilience.Resilience;
import org.asynchttpclient.Response;

import java.util.concurrent.CompletableFuture;

import static com.spark.constant.FilterConstant.ROUTE_FILTER_NAME;
import static com.spark.constant.FilterConstant.ROUTE_FILTER_ORDER;


public class RouteFilter implements Filter {

    /**
     * 执行预过滤操作
     * 此方法根据路由的弹性配置决定请求的处理方式如果启用了弹性配置，则使用弹性实例执行请求；
     * 否则，构建路由并处理请求，同时处理可能的异常
     *
     * @param context 网关上下文，包含路由和请求的相关信息
     */
    @Override
    public void doPreFilter(GatewayContext context) {
        // 获取路由的弹性配置
        RouteDefinition.ResilienceConfig resilience = context.getRoute().getResilience();

        // 判断是否启用弹性配置
        if (resilience.isEnabled()) {
            // 如果启用，使用弹性实例执行请求
            Resilience.getInstance().executeRequest(context);
        } else {
            // 如果未启用，构建路由并处理请求
            CompletableFuture<Response> future = RouteUtil.buildRouteSupplier(context).get().toCompletableFuture();

            // 处理可能的异常
            future.exceptionally(throwable -> {
                // 设置错误响应并回写给客户端
                context.setResponse(ResponseHelper.buildGatewayResponse(ResponseCode.HTTP_RESPONSE_ERROR));
                ContextHelper.writeBackResponse(context);
                return null;
            });
        }
    }

    @Override
    public void doPostFilter(GatewayContext context) {
        context.doFilter();
    }

    @Override
    public String mark() {
        return ROUTE_FILTER_NAME;
    }

    @Override
    public int getOrder() {
        return ROUTE_FILTER_ORDER;
    }

}
