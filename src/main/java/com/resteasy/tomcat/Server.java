package com.resteasy.tomcat;


import jakarta.ws.rs.core.Application;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Server {
    public static void main(String[] args) throws Exception {
        final Tomcat tomcat = getTomcat(8081);
        tomcat.start();
        tomcat.getServer().await();
    }

    protected static Tomcat getTomcat(int portNum) {
        final Tomcat tomcat = new Tomcat();
        tomcat.setPort(portNum);
        tomcat.getConnector();
        final Context ctx = tomcat.addContext("/", new File(".").getAbsolutePath());
        ctx.addParameter("jakarta.ws.rs.Application", JaxrsApplication.class.getName());
        Tomcat.addServlet(ctx, "rest-easy-servlet", new HttpServlet30Dispatcher());
        ctx.addServletMappingDecoded("/*", "rest-easy-servlet");
        return tomcat;
    }

    public static class JaxrsApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return new HashSet<>(Collections.singletonList(AsyncStreaming.class));
        }
    }
}
