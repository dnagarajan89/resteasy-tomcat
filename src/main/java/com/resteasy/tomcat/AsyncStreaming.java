package com.resteasy.tomcat;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

@Path("/stream")
public class AsyncStreaming {

    @POST
    @Path("/async")
    public void echoAsync(final InputStream requestBody, @Suspended final AsyncResponse resp) {
        CompletableFuture.supplyAsync(() -> Response.ok(requestBody).build())
                .thenAccept(resp::resume);
    }

}
