package com.resteasy.tomcat;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import org.apache.catalina.startup.Tomcat;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TomcatTest {

    private static final int PORT = 8083;
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

    public static Throwable getRootCause(Throwable cause) {
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private String generateLargeContent() {
        final Random random = new Random();
        final Function<Integer, String> charFn = (index) -> random.nextInt() % 2 == 0 ? "a" : "b";
        return IntStream.range(0, 50_000)
                .mapToObj(charFn::apply)
                .collect(Collectors.joining(","));
    }

    public void callStreaming(String path, int callCount) {

        final String inputData = generateLargeContent();
        final List<CompletableFuture<String>> futures = IntStream.range(0, callCount)
                .mapToObj(
                        x -> CompletableFuture.supplyAsync(() -> {
                            try {
                                return client.target(String.format("http://localhost:%s/stream/%s", PORT, path))
                                        .request()
                                        .post(Entity.text(inputData), String.class);
                            } catch (Exception ex) {
                                if(!(getRootCause(ex) instanceof SocketTimeoutException)) {
                                    return ex.getLocalizedMessage();
                                }
                                ex.printStackTrace();
                                return null;
                            }
                        })
                ).collect(Collectors.toList());

        List<String> completedCalls = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(
                        x -> futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).collect(Collectors.toList())
                ).join();

        Assert.assertNotNull(completedCalls);
        Assert.assertEquals("All calls complete", callCount, completedCalls.size());
        Assert.assertTrue("All data echoed", completedCalls.stream().allMatch(inputData::equals));
    }

    @Test
    public void testEchoAsync() {
        callStreaming("async", 1);
    }

    @Test
    public void testEchoAsyncParallel() {
        callStreaming("async", 10);
    }
}
