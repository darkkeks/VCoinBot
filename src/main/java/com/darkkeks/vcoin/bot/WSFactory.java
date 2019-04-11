package com.darkkeks.vcoin.bot;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class WSFactory {

    private static final int THREADS = 32;
    private static final int WSS_PORT = 443;
    private static final int RECONNECT_DELAY = 300;

    private Controller controller;
    private EventLoopGroup group;
    private final SslContext sslContext;

    public WSFactory(Controller controller) {
        this.controller = controller;
        group = new NioEventLoopGroup(THREADS);

        try {
            sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } catch (SSLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void connect(URI uri) {
        Bootstrap bootstrap = new Bootstrap();

        String host = uri.getHost();
        int port = WSS_PORT;

        WSClientHandler wsClientHandler = new WSClientHandler(
                WebSocketClientHandshakerFactory.newHandshaker(
                        uri, WebSocketVersion.V13, null,
                        true, new DefaultHttpHeaders()));

        int userId = Util.extractUserId(uri.getQuery());
        VCoinHandler vCoinHandler = new VCoinHandler(userId, controller);

        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();

                        pipeline.addLast(
                                sslContext.newHandler(ch.alloc(), host, port),
                                new HttpClientCodec(),
                                new HttpObjectAggregator(8192),
                                WebSocketClientCompressionHandler.INSTANCE,
                                wsClientHandler,
                                vCoinHandler
                        );
                    }
                });

        System.out.println("Connecting " + vCoinHandler.getId());
        bootstrap.connect(host, port).addListener((ChannelFuture future) -> {
            if(future.isSuccess()) {
                future.channel().closeFuture().addListener((ChannelFuture f) -> {
                    System.out.println("Disconnected " + vCoinHandler.getId());
                    scheduleReconnect(f, uri);
                });

                wsClientHandler.handshakeFuture().addListener(f -> {
                    if(f.isSuccess()) {
                        System.out.println("Handshaked " + vCoinHandler.getId());
                    } else {
                        System.out.println("Handshake failed " + vCoinHandler.getId());
                        f.cause().printStackTrace();
                    }
                });
            } else {
                future.cause().printStackTrace();
                System.out.println("Failed to connect " + vCoinHandler.getId());
                scheduleReconnect(future, uri);
            }
        });
    }

    private void scheduleReconnect(ChannelFuture future, URI uri) {
        future.channel().eventLoop().schedule(() -> connect(uri), RECONNECT_DELAY, TimeUnit.SECONDS);
    }

}
