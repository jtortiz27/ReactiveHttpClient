package com.ortiz.model;


import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.netty.http.client.HttpClientResponse;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestApiResult<T> {
    private T successResult;
    private List<T> successResults;
    private T errorResult;
    private boolean success;
    private String requestPath;
    private HttpHeaders headers;
    private HttpMethod httpMethod;
    private HttpClientResponse clientResponse;
}

