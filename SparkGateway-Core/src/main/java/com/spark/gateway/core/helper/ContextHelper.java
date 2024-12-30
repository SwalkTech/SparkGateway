package com.spark.gateway.core.helper;

import com.spark.gateway.config.helper.RouteResolver;
import com.spark.gateway.config.manager.DynamicConfigManager;
import com.spark.gateway.config.pojo.RouteDefinition;
import com.spark.gateway.core.context.GatewayContext;
import com.spark.gateway.core.request.GatewayRequest;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ContextHelper {

    /**
     * 构建网关上下文对象
     * 该方法根据传入的FullHttpRequest对象和ChannelHandlerContext对象，创建并返回一个GatewayContext对象
     * 主要用于处理来自客户端的请求，并为网关处理请求做准备
     *
     * @param request FullHttpRequest对象，包含客户端发起的HTTP请求的详细信息
     * @param ctx     ChannelHandlerContext对象，表示Netty框架中的通道处理上下文，用于处理网络通信
     * @return GatewayContext对象，封装了网关处理请求所需的上下文信息
     */
    public static GatewayContext buildGatewayContext(FullHttpRequest request, ChannelHandlerContext ctx) {
        // 根据请求的URI匹配路由定义，以便确定请求如何被处理
        RouteDefinition route = RouteResolver.matchingRouteByUri(request.uri());

        // 构建网关请求对象，这是根据匹配到的路由定义和服务名称，以及客户端的请求和通道上下文
        GatewayRequest gatewayRequest = RequestHelper.buildGatewayRequest(
                DynamicConfigManager.getInstance().getServiceByName(route.getServiceName()), request, ctx);

        // 创建并返回网关上下文对象，它包含了处理网关请求所需的所有信息：
        // 通道处理上下文、网关请求、路由定义和HTTP连接是否保持活跃
        return new GatewayContext(ctx, gatewayRequest, route, HttpUtil.isKeepAlive(request));
    }

    /**
     * 将响应写回到客户端
     * 根据上下文决定使用长连接还是短连接来写回响应
     *
     * @param context 网关上下文，包含响应信息和Netty上下文
     */
    public static void writeBackResponse(GatewayContext context) {
        // 构建HTTP响应对象
        FullHttpResponse httpResponse = ResponseHelper.buildHttpResponse(context.getResponse());

        // 判断是短连接还是长连接
        if (!context.isKeepAlive()) { // 短连接
            // 对于短连接，发送完数据后关闭通道
            context.getNettyCtx().writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
        } else { // 长连接
            // 对于长连接，设置响应头以保持连接
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            // 写入并刷新响应对象，但不关闭通道
            context.getNettyCtx().writeAndFlush(httpResponse);
        }
    }

}
