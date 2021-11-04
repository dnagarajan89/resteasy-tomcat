package com.resteasy.tomcat;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
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
