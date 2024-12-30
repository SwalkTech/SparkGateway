package com.spark.gateway.core.netty.processor;

import com.spark.enums.ResponseCode;
import com.spark.exception.GatewayException;
import com.spark.gateway.core.context.GatewayContext;
import com.spark.gateway.core.filter.FilterChainFactory;
import com.spark.gateway.core.helper.ContextHelper;
import com.spark.gateway.core.helper.ResponseHelper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Netty核心处理器，用于处理接收到的HTTP请求
 */
@Slf4j
public class NettyCoreProcessor implements NettyProcessor {

    /**
     * 处理HTTP请求的主要方法
     *
     * @param ctx     通道处理上下文，包含通道、管道等信息
     * @param request 接收到的完整HTTP请求对象
     */
    @Override
    public void process(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            // 构建网关上下文，包含请求、响应、过滤链等信息
            GatewayContext gatewayContext = ContextHelper.buildGatewayContext(request, ctx);
            // 构建过滤链
            FilterChainFactory.buildFilterChain(gatewayContext);

            // 执行过滤链
            gatewayContext.doFilter();

        } catch (GatewayException e) {
            // 处理网关异常
            log.error("处理错误 {} {}", e.getCode(), e.getCode().getMessage());
            // 构建错误响应
            FullHttpResponse httpResponse = ResponseHelper.buildHttpResponse(e.getCode());
            // 发送响应并释放资源
            doWriteAndRelease(ctx, request, httpResponse);
        } catch (Throwable t) {
            // 处理未知异常
            log.error("处理未知错误", t);
            // 构建内部错误响应
            FullHttpResponse httpResponse = ResponseHelper.buildHttpResponse(ResponseCode.INTERNAL_ERROR);
            // 发送响应并释放资源
            doWriteAndRelease(ctx, request, httpResponse);
        }
    }

    /**
     * 发送响应并释放资源
     *
     * @param ctx          通道处理上下文
     * @param request      HTTP请求对象
     * @param httpResponse HTTP响应对象
     */
    private void doWriteAndRelease(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse httpResponse) {
        // 写入响应并冲刷缓冲区，同时添加关闭通道的监听器
        ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
        // 释放请求资源
        ReferenceCountUtil.release(request);
    }

}
