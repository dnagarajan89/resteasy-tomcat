package com.resteasy.tomcat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Path("/stream")
public class AsyncStreaming {

    @POST
    @Path("/async")
    public void echoAsync(final InputStream requestBody, @Suspended final AsyncResponse resp) {
        CompletableFuture.supplyAsync(() -> Response.ok(requestBody).build())
                .thenAccept(resp::resume);
    }

    @POST
    @Path("/async/reactive/{noOfItems}")
    @Produces(MediaType.APPLICATION_JSON)
    public Mono<List<ResponseData>> echoAsyncReactive(@PathParam("noOfItems") final int noOfItems,
                                                      final String requestBody) {
        final Map<String, String> headers = IntStream.range(1, 5)
                .mapToObj(i -> new String[]{ "testHeader" + i, "testHeaderValue" + i})
                .collect(Collectors.toMap(data -> data[0], data -> data[1]));
        return Flux.range(0, noOfItems)
                .flatMap(index -> Mono
                        .just(new ResponseData(index, requestBody + "_response", headers))
                        .delayElement(Duration.ofMillis(1))
                ).collectList();
    }

    public static class ResponseData {
        private final Integer id;
        private final String responseBody;
        private final Map<String, String> headers;


        public ResponseData(final Integer id,
                            final String responseBody,
                            final Map<String, String> headers) {
            this.id = id;
            this.responseBody = responseBody;
            this.headers = headers;
        }

        public Integer getId() {
            return id;
        }

        public String getResponseBody() {
            return responseBody;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }
    }

}
