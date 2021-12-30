package com.resteasy.tomcat;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/stream")
public class AsyncStreaming {

    @POST
    @Path("/async")
    public void echoAsync(final InputStream requestBody, @Suspended final AsyncResponse resp) {
        CompletableFuture.supplyAsync(() -> Response.ok(requestBody).build())
                .thenAccept(resp::resume);
    }

    @POST
    @Path("/async/reactive/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Mono<ResponseHolder> echoAsyncReactive(@PathParam("id") final String id, final String requestBody) {
        final Map<String, String> headers = Map.of(
                "testHeader1", "testHeaderValue1",
                "testHeader2", "testHeaderValue2",
                "testHeader3", "testHeaderValue3",
                "testHeader4", "testHeaderValue4"
        );
        return Flux.range(0, 50)
                .flatMap(i -> Mono.just(createResponseObject(requestBody, id, headers)).delayElement(Duration.ofMillis(50)))
                .collectList()
                .map(list ->  {
                    final AtomicInteger counter = new AtomicInteger(0);
                    final Map<String, ResponseData> responses = list
                            .stream()
                            .collect(Collectors.toMap(e -> String.valueOf(counter.incrementAndGet()), Function.identity()));
                    return new ResponseHolder(requestBody, headers, responses);
                });
    }

    private ResponseData createResponseObject(final String requestBody,
                                              final String id,
                                              final Map<String, String> headers) {

        return new ResponseData(requestBody + "_response", headers);
    }


    // Some big object that helps reproduce the issue during concurrent calls.
    public static class ResponseHolder {
        private final String requestData;
        private final Map<String, String> headers;
        private final Map<String, ResponseData> responses;
        public ResponseHolder(final String requestData,
                              final Map<String, String> headers,
                              final Map<String, ResponseData> responses) {
            this.requestData = requestData;
            this.responses = responses;
            this.headers = headers;
        }

        public String getRequestData() {
            return requestData;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public Map<String, ResponseData> getResponses() {
            return responses;
        }
    }

    public static class ResponseData {
        private final String responseBody;
        private final Map<String, String> headers;


        public ResponseData(final String responseBody,
                            final Map<String, String> headers) {
            this.responseBody = responseBody;
            this.headers = headers;
        }

        public String getResponseBody() {
            return responseBody;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }
    }

}
