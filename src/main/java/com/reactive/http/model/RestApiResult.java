package com.reactive.http.model;


import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClientResponse;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestApiResult<T> {
    private T successResult;
    private List<T> successResults;
    private ErrorResponse errorResponse;
    private boolean success;
    private boolean isMultiple;
    private String requestPath;
    private HttpHeaders headers;
    private HttpMethod httpMethod;
    private HttpClientResponse clientResponse;
    private HttpResponseStatus httpResponseStatus;
    private int httpStatusCode;
}

