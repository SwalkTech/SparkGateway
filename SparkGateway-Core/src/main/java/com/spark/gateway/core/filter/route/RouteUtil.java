package com.spark.gateway.core.filter.route;

import com.spark.gateway.core.context.GatewayContext;
import com.spark.gateway.core.helper.ResponseHelper;
import com.spark.gateway.core.http.HttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class RouteUtil {

    /**
     * 构建一个供应异步路由处理结果的供应商函数
     * 该方法用于创建并返回一个供应商函数，该供应商函数在被调用时会发起HTTP请求，并处理响应
     *
     * @param context 网关上下文，包含请求和响应处理所需的信息
     * @return 一个供应商函数，它会异步执行请求并返回一个完成阶段，包含响应
     */
    public static Supplier<CompletionStage<Response>> buildRouteSupplier(GatewayContext context) {
        return () -> {
            // 构建请求对象
            Request request = context.getRequest().build();
            // 执行HTTP请求并获取异步结果
            CompletableFuture<Response> future = HttpClient.getInstance().executeRequest(request);
            // 完成时处理响应或异常
            future.whenComplete(((response, throwable) -> {
                // 如果发生异常，设置上下文中的异常并抛出运行时异常
                if (throwable != null) {
                    context.setThrowable(throwable);
                    throw new RuntimeException(throwable);
                }
                // 处理正常响应，设置上下文中的响应，并执行过滤器
                context.setResponse(ResponseHelper.buildGatewayResponse(response));
                context.doFilter();
            }));
            // 返回异步结果
            return future;
        };
    }

}
