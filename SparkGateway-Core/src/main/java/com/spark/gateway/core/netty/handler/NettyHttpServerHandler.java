package com.spark.gateway.core.netty.handler;

import com.spark.gateway.core.netty.processor.NettyProcessor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Netty HTTP服务器处理器
 * 继承自ChannelInboundHandlerAdapter，用于处理HTTP请求
 * 主要职责是将接收到的HTTP请求转发给NettyProcessor进行处理
 */
public class NettyHttpServerHandler extends ChannelInboundHandlerAdapter {
    // Netty处理器，用于处理HTTP请求
    private final NettyProcessor nettyProcessor;

    /**
     * 构造函数，初始化NettyHttpServerHandler
     *
     * @param nettyProcessor Netty处理器，用于处理接收到的HTTP请求
     */
    public NettyHttpServerHandler(NettyProcessor nettyProcessor) {
        this.nettyProcessor = nettyProcessor;
    }

    /**
     * 读取通道数据时触发的方法
     * 当通道读取到数据时，此方法会被调用
     *
     * @param ctx 通道处理上下文，包含了通道、管道等信息
     * @param msg 读取到的数据，这里应该是FullHttpRequest类型的HTTP请求
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 调用NettyProcessor的process方法处理HTTP请求
        nettyProcessor.process(ctx, (FullHttpRequest) msg);
    }

    /**
     * 捕获到异常时触发的方法
     * 当处理过程中出现异常时，此方法会被调用
     *
     * @param ctx   通道处理上下文
     * @param cause 异常原因
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 调用父类的 exceptionCaught 方法，它将按照 ChannelPipeline 中的下一个处理器继续处理异常
        super.exceptionCaught(ctx, cause);
    }

}
