package org.jboss.resteasy.client.jaxrs.engines;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;

/**
 * An Apache HTTP engine for use with the new Builder Config style.
 * To accommodate the configuration style this class overrides the methods that get/set from the old Parameters
 * Otherwise only the old style parameters will get picked up.
 *
 * Setting a proxy is NOT supported.  If you need to set a proxy please pass in a pre-configured HTTP client.
 *
 * Consider using the factory ApacheHttpClient4EngineFactory instead of using this class directly
 */
public class ApacheHttpClient4ConfigStyleEngine extends ApacheHttpClient4Engine
{

    public ApacheHttpClient4ConfigStyleEngine()
    {
        super();
    }

    public ApacheHttpClient4ConfigStyleEngine(final HttpClient httpClient) {
        super(httpClient);
    }

    public ApacheHttpClient4ConfigStyleEngine(final HttpClient httpClient, final boolean closeHttpClient)
    {
        super(httpClient, closeHttpClient);
    }

    public ApacheHttpClient4ConfigStyleEngine(final HttpClient httpClient, final HttpContext httpContext)
    {
        super(httpClient, httpContext);
    }

    @Override
    protected HttpClient createDefaultHttpClient()
    {
        final HttpClientBuilder builder = HttpClientBuilder.create();
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        builder.setDefaultRequestConfig(requestBuilder.build());
        return builder.build();
    }

    @Override
    public HttpHost getDefaultProxy()
    {
        Configurable clientConfiguration = (Configurable) httpClient;
        return clientConfiguration.getConfig().getProxy();
    }

    @Override
    public void setDefaultProxy(final HttpHost defaultProxy)
    {
        throw new IllegalArgumentException("Cannot set proxy.  Please pass in a pre-configured httpclient instead.");
    }

    @Override
    protected void setRedirectRequired(final ClientInvocation request, final HttpRequestBase httpMethod)
    {
        RequestConfig.Builder requestBuilder = RequestConfig.copy(getCurrentConfiguration(httpMethod));
        requestBuilder.setRedirectsEnabled(true);
        httpMethod.setConfig(requestBuilder.build());
    }

    @Override
    protected void setRedirectNotRequired(final ClientInvocation request, final HttpRequestBase httpMethod)
    {
        RequestConfig.Builder requestBuilder = RequestConfig.copy(getCurrentConfiguration(httpMethod));
        requestBuilder.setRedirectsEnabled(false);
        httpMethod.setConfig(requestBuilder.build());
    }

    private RequestConfig getCurrentConfiguration(final HttpRequestBase httpMethod)
    {
        RequestConfig baseConfig;
        if (httpMethod != null && httpMethod.getConfig() != null)
        {
            baseConfig = httpMethod.getConfig();
        }
        else
        {
            Configurable clientConfiguration = (Configurable) httpClient;
            baseConfig = clientConfiguration.getConfig();
        }
        return baseConfig;
    }
}
