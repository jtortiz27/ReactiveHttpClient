package com.reactive.http.model;

import io.netty.handler.codec.http.HttpStatusClass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.netty.http.client.HttpClientResponse;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private String errorCode;
    private String errorDescription;
    private HttpStatusClass httpStatus;
    private HttpClientResponse clientResponse;
    private Exception exception;
}
