package org.jboss.resteasy.plugins.server.netty;

import static org.jboss.resteasy.plugins.server.netty.RestEasyHttpRequestDecoder.Protocol.HTTP;
import static org.jboss.resteasy.plugins.server.netty.RestEasyHttpRequestDecoder.Protocol.HTTPS;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.EventExecutor;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.plugins.server.embedded.EmbeddedJaxrsServer;
import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.spi.ResteasyDeployment;

/**
 * An HTTP server that sends back the content of the received HTTP request
 * in a pretty plaintext form.
 *
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author Andy Taylor (andy.taylor@jboss.org)
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 * @author Norman Maurer
 * @version $Rev: 2080 $, $Date: 2010-01-26 18:04:19 +0900 (Tue, 26 Jan 2010) $
 */
public class NettyJaxrsServer implements EmbeddedJaxrsServer
{
   protected ServerBootstrap bootstrap = new ServerBootstrap();
   protected String hostname = null;
   protected int port = 8080;
   protected ResteasyDeployment deployment = new ResteasyDeployment();
   protected String root = "";
   protected SecurityDomain domain;
   private EventLoopGroup eventLoopGroup;
   private EventLoopGroup eventExecutor;
   private int ioWorkerCount = Runtime.getRuntime().availableProcessors() * 2;
   private int executorThreadCount = 16;
   private SSLContext sslContext;
   private int maxRequestSize = 1024 * 1024 * 10;
   private int maxInitialLineLength = 4096;
   private int maxHeaderSize = 8192;
   private int maxChunkSize = 8192;
   private int backlog = 128;
   private List<ChannelHandler> channelHandlers = Collections.emptyList();
   private Map<ChannelOption, Object> channelOptions = Collections.emptyMap();
   private Map<ChannelOption, Object> childChannelOptions = Collections.emptyMap();
   private List<ChannelHandler> httpChannelHandlers = Collections.emptyList();

   public void setSSLContext(SSLContext sslContext)
   {
      this.sslContext = sslContext;
   }

   /**
    * Specify the worker count to use. For more information about this please see the javadocs of {@link EventLoopGroup}
    *
    * @param ioWorkerCount
    */
   public void setIoWorkerCount(int ioWorkerCount)
   {
       this.ioWorkerCount = ioWorkerCount;
   }

   /**
    * Set the number of threads to use for the EventExecutor. For more information please see the javadocs of {@link EventExecutor}.
    * If you want to disable the use of the {@link EventExecutor} specify a value <= 0.  This should only be done if you are 100% sure that you don't have any blocking
    * code in there.
    *
    * @param executorThreadCount
    */
   public void setExecutorThreadCount(int executorThreadCount)
   {
       this.executorThreadCount = executorThreadCount;
   }

    /**
     * Set the max. request size in bytes. If this size is exceed we will send a "413 Request Entity Too Large" to the client.
     *
     * @param maxRequestSize the max request size. This is 10mb by default.
     */
    public void setMaxRequestSize(int maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    public void setMaxInitialLineLength(int maxInitialLineLength) {
        this.maxInitialLineLength = maxInitialLineLength;
    }

    public void setMaxHeaderSize(int maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
    }

    public void setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setBacklog(int backlog)
    {
        this.backlog = backlog;
    }

    /**
     * Add additional {@link io.netty.channel.ChannelHandler}s to the {@link io.netty.bootstrap.ServerBootstrap}.
     * <p>The additional channel handlers are being added <em>before</em> the HTTP handling.</p>
     *
     * @param channelHandlers the additional {@link io.netty.channel.ChannelHandler}s.
     */
    public void setChannelHandlers(final List<ChannelHandler> channelHandlers) {
        this.channelHandlers = channelHandlers == null ? Collections.<ChannelHandler>emptyList() : channelHandlers;
    }

    /**
     * Add additional {@link io.netty.channel.ChannelHandler}s to the {@link io.netty.bootstrap.ServerBootstrap}.
     * <p>The additional channel handlers are being added <em>after</em> the HTTP handling.</p>
     *
     * @param httpChannelHandlers the additional {@link io.netty.channel.ChannelHandler}s.
     */
    public void setHttpChannelHandlers(final List<ChannelHandler> httpChannelHandlers) {
        this.httpChannelHandlers = httpChannelHandlers == null ? Collections.<ChannelHandler>emptyList() : httpChannelHandlers;
    }

    /**
     * Add Netty {@link io.netty.channel.ChannelOption}s to the {@link io.netty.bootstrap.ServerBootstrap}.
     *
     * @param channelOptions the additional {@link io.netty.channel.ChannelOption}s.
     * @see io.netty.bootstrap.ServerBootstrap#option(io.netty.channel.ChannelOption, Object)
     */
    public void setChannelOptions(final Map<ChannelOption, Object> channelOptions) {
        this.channelOptions = channelOptions == null ? Collections.<ChannelOption, Object>emptyMap() : channelOptions;
    }

    /**
     * Add child options to the {@link io.netty.bootstrap.ServerBootstrap}.
     *
     * @param channelOptions the additional child {@link io.netty.channel.ChannelOption}s.
     * @see io.netty.bootstrap.ServerBootstrap#childOption(io.netty.channel.ChannelOption, Object)
     */
    public void setChildChannelOptions(final Map<ChannelOption, Object> channelOptions) {
        this.childChannelOptions = channelOptions == null ? Collections.<ChannelOption, Object>emptyMap() : channelOptions;
    }

   @Override
   public void setDeployment(ResteasyDeployment deployment)
   {
      this.deployment = deployment;
   }

   @Override
   public void setRootResourcePath(String rootResourcePath)
   {
      root = rootResourcePath;
      if (root != null && root.equals("/")) root = "";
   }

   @Override
   public ResteasyDeployment getDeployment()
   {
      return deployment;
   }

   @Override
   public void setSecurityDomain(SecurityDomain sc)
   {
      this.domain = sc;
   }

   protected RequestDispatcher createRequestDispatcher()
   {
       return new RequestDispatcher((SynchronousDispatcher)deployment.getDispatcher(),
               deployment.getProviderFactory(), domain);
   }

    @Override
    public void start() {
        eventLoopGroup = new NioEventLoopGroup(ioWorkerCount);
        eventExecutor = new NioEventLoopGroup(executorThreadCount);
        deployment.start();
        // Configure the server.
        bootstrap.group(eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(createChannelInitializer())
                .option(ChannelOption.SO_BACKLOG, backlog)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        for (Map.Entry<ChannelOption, Object> entry : channelOptions.entrySet()) {
            bootstrap.option(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<ChannelOption, Object> entry : childChannelOptions.entrySet()) {
            bootstrap.childOption(entry.getKey(), entry.getValue());
        }

        final InetSocketAddress socketAddress;
        if (null == hostname || hostname.isEmpty()) {
            socketAddress = new InetSocketAddress(port);
        } else {
            socketAddress = new InetSocketAddress(hostname, port);
        }

        bootstrap.bind(socketAddress).syncUninterruptibly();
    }

    private ChannelInitializer<SocketChannel> createChannelInitializer() {
        final RequestDispatcher dispatcher = createRequestDispatcher();
        if (sslContext == null) {
            return new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    setupHandlers(ch, dispatcher, HTTP);
                }
            };
        } else {
            final SSLEngine engine = sslContext.createSSLEngine();
            engine.setUseClientMode(false);
            return new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addFirst(new SslHandler(engine));
                    setupHandlers(ch, dispatcher, HTTPS);
                }
            };
        }
    }

    private void setupHandlers(SocketChannel ch, RequestDispatcher dispatcher, RestEasyHttpRequestDecoder.Protocol protocol) {
        ChannelPipeline channelPipeline = ch.pipeline();
        channelPipeline.addLast(channelHandlers.toArray(new ChannelHandler[channelHandlers.size()]));
        channelPipeline.addLast(new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize));
        channelPipeline.addLast(new HttpObjectAggregator(maxRequestSize));
        channelPipeline.addLast(new HttpResponseEncoder());
        channelPipeline.addLast(httpChannelHandlers.toArray(new ChannelHandler[httpChannelHandlers.size()]));
        channelPipeline.addLast(new RestEasyHttpRequestDecoder(dispatcher.getDispatcher(), root, protocol));
        channelPipeline.addLast(new RestEasyHttpResponseEncoder());
        channelPipeline.addLast(eventExecutor, new RequestHandler(dispatcher));
    }

   @Override
   public void stop()
   {
       eventLoopGroup.shutdownGracefully();
       eventExecutor.shutdownGracefully();
   }
}
