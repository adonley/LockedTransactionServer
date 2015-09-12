package network.bitmesh.cloudserver.Servlets;

import network.bitmesh.cloudserver.ServerConfig;

import java.net.MalformedURLException;
import java.net.URL;

public class TestConfig
{
    public static final String port;
    public static final String ip;

    static
    {
        if(ServerConfig.BITMESH_TEST)
        {
            // This has to be the same as your local tomcat config for testing
            port = "8082";
            ip = "127.0.0.1";
        }
        else
        {
            port = "80";
            ip = "www.bitmesh.network";
        }
    }

    public static URL urlBuilder(String route)
    {
        try
        {
            return new URL("http://" + TestConfig.ip + ":" + TestConfig.port + "/" + route);
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
