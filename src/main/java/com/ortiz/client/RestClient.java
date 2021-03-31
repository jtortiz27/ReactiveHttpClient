package com.ortiz.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ortiz.model.ApiResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class RestClient {

    private static final HttpClient httpClient = HttpClient.create();
    private static final ObjectMapper mapper = new JsonMapper();

    public static <T> Mono<ApiResult<T>> getResourceAsync(String url, final Class<T> returnType) throws Exception {
        ApiResult<T> apiResult = new ApiResult<>();
        try {
            return httpClient.get()
                    .uri(url)
                    .responseSingle(((httpClientResponse, byteBufFlux) -> {

                        apiResult.setRequestPath(httpClientResponse.fullPath());
                        apiResult.setHeaders(httpClientResponse.requestHeaders());
                        apiResult.setHttpMethod(httpClientResponse.method());

                        int responseStatus = httpClientResponse.status().code();
                        if (responseStatus >= 200 && responseStatus < 300) {
                            apiResult.setSuccess(true);
                            httpClientResponse.resourceUrl();
                            return byteBufFlux.asString();
                        }

                        return Mono.error(new Exception("Received Error Status Code"));

                    }))
                    .log(null, Level.INFO, SignalType.ON_NEXT)
                    .flatMap(s -> {
                        try {
                            apiResult.setSuccessResult(deserialize(s.getBytes(StandardCharsets.UTF_8), returnType));
                            return Mono.just(apiResult);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return Mono.error(e);
                        }
                    }).single();
        } catch (Exception e) {
            return Mono.error(new Exception("Excpetion occured while retrieving resource"));
        }
    }

    public static <T> Flux<ApiResult<T>> getResourcesAsync(String url, final Class<T> returnType) throws Exception {
        ApiResult<T> apiResult = new ApiResult<>();
        try {
            return httpClient.get()
                    .uri(url)
                    .response(((httpClientResponse, byteBufFlux) ->  {

                        apiResult.setRequestPath(httpClientResponse.fullPath());
                        apiResult.setHeaders(httpClientResponse.requestHeaders());
                        apiResult.setHttpMethod(httpClientResponse.method());

                        int responseStatus = httpClientResponse.status().code();
                        if (responseStatus >= 200 && responseStatus < 300) {
                            apiResult.setSuccess(true);
                            return byteBufFlux.asString();
                        }

                        return Mono.error(new Exception("Received Error Status Code"));

                    }))
                    .log(null, Level.INFO, SignalType.ON_NEXT)
                    .flatMap(s -> {
                        try {
                            apiResult.setSuccessResult(deserializeList(s.getBytes(StandardCharsets.UTF_8), returnType));
                            return Flux.just(apiResult);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return Mono.error(e);
                        }
                    });
        } catch (Exception e) {
            return Flux.empty();
        }
    }

    private static <T> T deserialize(byte [] jsonBytes, Class<T> returnType) throws Exception {
        JsonParser asyncParser = mapper.getFactory().createNonBlockingByteArrayParser();
        ByteArrayFeeder feeder = (ByteArrayFeeder) asyncParser.getNonBlockingInputFeeder();
        feeder.feedInput(jsonBytes, 0, jsonBytes.length);
        feeder.endOfInput();

        return mapper.readValue(asyncParser, returnType);
    }
    private static <T> T deserializeList(byte [] jsonBytes, Class<T> returnType) throws Exception {
        JsonParser asyncParser = mapper.getFactory().createNonBlockingByteArrayParser();
        ByteArrayFeeder feeder = ((ByteArrayFeeder) asyncParser.getNonBlockingInputFeeder());
        feeder.feedInput(jsonBytes, 0, jsonBytes.length);
        feeder.endOfInput();
        return mapper.readerForArrayOf(returnType).readValue(jsonBytes);
    }
}
