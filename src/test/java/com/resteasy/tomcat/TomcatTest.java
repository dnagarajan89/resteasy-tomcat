package com.resteasy.tomcat;

import org.apache.catalina.startup.Tomcat;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TomcatTest {

    private static final int PORT = 8283;
    private static final Tomcat tomcat = Server.getTomcat(PORT);
    private static Client client;

    @BeforeClass
    public static void init() throws Exception
    {
        tomcat.start();
        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        clientBuilder.readTimeout(30, TimeUnit.SECONDS);
        client = clientBuilder.build();

    }

    @AfterClass
    public static void stop() throws Exception
    {
        client.close();
        tomcat.stop();
    }

    @Test
    public void testEchoAsyncReactive() {
        final int numberOfCalls = 500;
        final int numberOfItems = 200;

        final List<CompletableFuture<Void>> responses = IntStream.range(0, numberOfCalls)
                .mapToObj(iteration ->
                        CompletableFuture.supplyAsync(
                                () -> client
                                        .target(String.format("http://localhost:%s/stream/async/reactive/%s", PORT, numberOfItems))
                                        .request()
                                        .post(Entity.text("Test Request " + iteration)))
                                .thenAccept(this::bufferAndCloseRepopnse)
                ).collect(Collectors.toList());

        CompletableFuture.allOf(responses.toArray(new CompletableFuture[responses.size()])).join();
    }

    private void bufferAndCloseRepopnse(final Response response) {
        response.bufferEntity();
        response.close();
    }
}
