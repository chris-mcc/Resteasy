package org.jboss.resteasy.client.jaxrs.engines.factory;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.protocol.HttpContext;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4ConfigStyleEngine;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

/**
 * This factory determines what Engine should be used with the supplied httpClient
 * If no Httpclient is specified we use the new config style engine if allowed
 */
public class ApacheHttpClient4EngineFactory
{
    private ApacheHttpClient4EngineFactory()
    {

    }

    public static ClientHttpEngine create()
    {
        if(isConfigurableAvailable())
        {
            ApacheHttpClient4Engine engine = new ApacheHttpClient4ConfigStyleEngine();
            //We have to check that the HttpClient to be used has the configurable interface
            if(isUsingOldStyleConfiguration(engine.getHttpClient()))
            {
              engine.close();
              return new ApacheHttpClient4Engine();
            }
            return engine;
        }
        else
        {
            return new ApacheHttpClient4Engine();
        }
    }

    public static ClientHttpEngine create(HttpClient httpClient)
    {
        if(isUsingOldStyleConfiguration(httpClient))
        {
            return new ApacheHttpClient4Engine(httpClient);
        }
        else
        {
            return new ApacheHttpClient4ConfigStyleEngine(httpClient);
        }
    }

    public static ClientHttpEngine create(HttpClient httpClient, boolean closeHttpClient)
    {
        if(isUsingOldStyleConfiguration(httpClient))
        {
            return new ApacheHttpClient4Engine(httpClient,closeHttpClient);
        }
        else
        {
            return new ApacheHttpClient4ConfigStyleEngine(httpClient,closeHttpClient);
        }
    }

    public static ClientHttpEngine create(HttpClient httpClient, HttpContext httpContext)
    {
        if(isUsingOldStyleConfiguration(httpClient))
        {
            return new ApacheHttpClient4Engine(httpClient,httpContext);
        }
        else
        {
            return new ApacheHttpClient4ConfigStyleEngine(httpClient,httpContext);
        }
    }

    private static boolean isUsingOldStyleConfiguration(HttpClient client)
    {
        if(!isConfigurableAvailable())
        {
            return true;
        }

        if(!(client instanceof Configurable)) // Yep, they could be using a new style config with a client that we can't actually use
        {
            return true;
        }

        RequestConfig config = ((Configurable) client).getConfig();
        return config == null;
    }

    private static boolean isConfigurableAvailable()
    {
        try
        {
            Class.forName("org.apache.http.client.methods.Configurable");
            return true;
        }
        catch (ClassNotFoundException e)
        {
           return false;
        }
    }
}
