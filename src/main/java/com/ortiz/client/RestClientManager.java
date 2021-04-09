package com.ortiz.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.ortiz.model.ErrorResponse;
import com.ortiz.model.RestApiResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpStatusClass;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

public class RestClientManager {

    private static final HttpClient httpClient = HttpClient.create();
    private static final ObjectMapper mapper = new JsonMapper();

    public RestClientManager() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * This Method should be used to make an HTTP GET call and shall deserialize the result to whatever class passed in <T>
     * <p>
     * Note: This is for retrieving a single resource and upon success, the deserialized result will be available in APIResult.getSuccessResult()
     *
     * @param url        url of resource to retrieve
     * @param returnType Type to deserialize to
     * @return APIResult with deserialized value of response
     */
    public <T> Mono<RestApiResult<T>> getResourceAsync(String url, Class<T> returnType) {
        RestApiResult<T> restApiResult = new RestApiResult<>();
        try {
            return httpClient.get()
                    .uri(url)
                    .responseSingle(((httpClientResponse, byteBufMono) -> {

                        //Populate Request metadata
                        restApiResult.setRequestPath(httpClientResponse.fullPath());
                        restApiResult.setHeaders(httpClientResponse.requestHeaders());
                        restApiResult.setHttpMethod(httpClientResponse.method());
                        restApiResult.setClientResponse(httpClientResponse);

                        //Determine if success
                        int responseStatus = httpClientResponse.status().code();

                        //If success, emit response as string to reactive flow
                        if (responseStatus >= 200 && responseStatus < 300) {
                            restApiResult.setSuccess(true);
                            return byteBufMono.asString();
                        }

                        //If error status code, populate ErrorResponse with appropriate HTTP level details and emit empty string
                        populateHttpErrorDetails(restApiResult, responseStatus, httpClientResponse);
                        return Mono.just("");
                    }))
                    .log(null, Level.INFO, SignalType.ON_NEXT) // log when data comes through pipeline
                    .filter(s -> !s.isBlank())
                    .flatMap(s -> {
                        try {
                            //Attempt to deserialize and return ApiResult
                            restApiResult.setSuccessResult(deserialize(s.getBytes(StandardCharsets.UTF_8), returnType));
                            return Mono.just(restApiResult);
                        } catch (Exception e) {
                            populateErrorDetails(restApiResult, e);
                            e.printStackTrace();
                            return Mono.error(e);
                        }
                    }).single();
        } catch (Exception e) {
            populateErrorDetails(restApiResult, e);
            return Mono.just(restApiResult);
        }
    }

    /**
     * This Method should be used to make an HTTP GET call and shall deserialize the result to whatever class passed in <T>
     * <p>
     * Note: This is for retrieving a list of resources and upon success, the deserialized result will be available in APIResult.getSuccessResults()
     *
     * @param url        url of resources to retrieve
     * @param returnType Type to deserialize list to
     * @return APIResult with deserialized value of responses
     */
    public <T> Mono<RestApiResult<T>> getResourcesAsync(String url, Class<T> returnType) {
        RestApiResult<T> restApiResult = new RestApiResult<>();
        try {
            return httpClient.get()
                    .uri(url)
                    .responseSingle(((httpClientResponse, byteBufMono) -> {

                        //Populate Request metadata
                        restApiResult.setRequestPath(httpClientResponse.fullPath());
                        restApiResult.setHeaders(httpClientResponse.requestHeaders());
                        restApiResult.setHttpMethod(httpClientResponse.method());
                        restApiResult.setClientResponse(httpClientResponse);

                        //Determine if success
                        int responseStatus = httpClientResponse.status().code();

                        //If success, emit response as string to reactive flow
                        if (responseStatus >= 200 && responseStatus < 300) {
                            restApiResult.setSuccess(true);
                            return byteBufMono.asString();
                        }

                        //If error status code, populate ErrorResponse with appropriate HTTP level details and emit empty string
                        populateHttpErrorDetails(restApiResult, responseStatus, httpClientResponse);
                        return Mono.just("");

                    }))
                    .log(null, Level.INFO, SignalType.ON_NEXT) // log when data comes through pipeline
                    .filter(s -> !s.isBlank())
                    .flatMap(s -> {
                        try {
                            //Attempt to deserialize records and return ApiResult
                            restApiResult.setSuccessResults(deserializeToList(s.getBytes(StandardCharsets.UTF_8), returnType));
                            return Mono.just(restApiResult);
                        } catch (Exception e) {
                            populateErrorDetails(restApiResult, e);
                            e.printStackTrace();
                            return Mono.error(e);
                        }
                    });
        } catch (Exception e) {
            populateErrorDetails(restApiResult, e);
            return Mono.just(restApiResult);
        }
    }

    /**
     * This Method should be used to make an HTTP POST call and shall deserialize the result to whatever class passed in <T>
     * <p>
     * Note: This is for retrieving a list of resources and upon success, the deserialized result will be available in APIResult.getSuccessResult()
     *
     * @param url          url of resources to retrieve
     * @param objectToPost Type to deserialize list to
     * @return APIResult with deserialized value of responses
     */
    @SuppressWarnings("unchecked")
    public <T> Mono<RestApiResult<T>> postResourceAsync(String url, T objectToPost) {
        RestApiResult<T> restApiResult = new RestApiResult<>();
        try {
            String postData = mapper.writeValueAsString(objectToPost);
            ByteBuf postBodyByteBuf = Unpooled.copiedBuffer(postData.getBytes());
            httpClient.post()
                    .uri(url)
                    .send(Mono.just(postBodyByteBuf))
                    .responseSingle((httpClientResponse, byteBufMono) -> {
                        //Populate Request metadata
                        restApiResult.setRequestPath(httpClientResponse.fullPath());
                        restApiResult.setHeaders(httpClientResponse.requestHeaders());
                        restApiResult.setHttpMethod(httpClientResponse.method());
                        restApiResult.setClientResponse(httpClientResponse);

                        //Determine if success
                        int responseStatus = httpClientResponse.status().code();

                        //If success, emit response as string to reactive flow
                        if (responseStatus >= 200 && responseStatus < 300) {
                            restApiResult.setSuccess(true);
                            return byteBufMono.asString();
                        }

                        //If error status code, populate ErrorResponse with appropriate HTTP level details and emit empty string
                        populateHttpErrorDetails(restApiResult, responseStatus, httpClientResponse);
                        return Mono.just("");
                    })
                    .log(null, Level.INFO, SignalType.ON_NEXT) // log when data comes through pipeline
                    .filter(s -> !s.isBlank())
                    .flatMap(s -> {
                        try {
                            //Attempt to deserialize and return ApiResult
                            restApiResult.setSuccessResult(deserialize(s.getBytes(StandardCharsets.UTF_8), (Class<T>) (objectToPost.getClass())));
                            return Mono.just(restApiResult);
                        } catch (Exception e) {
                            populateErrorDetails(restApiResult, e);
                            e.printStackTrace();
                            return Mono.error(e);
                        }
                    }).single();
            return Mono.just(restApiResult);
        } catch (Exception e) {
            populateErrorDetails(restApiResult, e);
            return Mono.just(restApiResult);
        }
    }

    /**
     * This Method should be used to make an HTTP PATCH call and shall deserialize the result to whatever class passed in <T>
     * <p>
     * Note: This is for patching a resource and upon success, the deserialized result will be available in APIResult.getSuccessResult()
     *
     * @param url           url of resources to patch
     * @param objectToPatch Type to deserialize list to
     * @return APIResult with deserialized value of patched resource
     */
    @SuppressWarnings("unchecked")
    public <T> Mono<RestApiResult<T>> patchResourceAsync(String url, T objectToPatch) {
        RestApiResult<T> restApiResult = new RestApiResult<>();
        try {
            String postData = mapper.writeValueAsString(objectToPatch);
            ByteBuf postBodyByteBuf = Unpooled.copiedBuffer(postData.getBytes());
            httpClient.patch()
                    .uri(url)
                    .send(Mono.just(postBodyByteBuf))
                    .responseSingle((httpClientResponse, byteBufMono) -> {
                        //Populate Request metadata
                        restApiResult.setRequestPath(httpClientResponse.fullPath());
                        restApiResult.setHeaders(httpClientResponse.requestHeaders());
                        restApiResult.setHttpMethod(httpClientResponse.method());
                        restApiResult.setClientResponse(httpClientResponse);

                        //Determine if success
                        int responseStatus = httpClientResponse.status().code();

                        //If success, emit response as string to reactive flow
                        if (responseStatus >= 200 && responseStatus < 300) {
                            restApiResult.setSuccess(true);
                            return byteBufMono.asString();
                        }

                        //If error status code, populate ErrorResponse with appropriate HTTP level details and emit empty string
                        populateHttpErrorDetails(restApiResult, responseStatus, httpClientResponse);
                        return Mono.just("");
                    })
                    .log(null, Level.INFO, SignalType.ON_NEXT) // log when data comes through pipeline
                    .filter(s -> !s.isBlank())
                    .flatMap(s -> {
                        try {
                            //Attempt to deserialize and return ApiResult
                            restApiResult.setSuccessResult(deserialize(s.getBytes(StandardCharsets.UTF_8), (Class<T>) (objectToPatch.getClass())));
                            return Mono.just(restApiResult);
                        } catch (Exception e) {
                            e.printStackTrace();
                            populateErrorDetails(restApiResult, e);
                            return Mono.error(e);
                        }
                    });
            return Mono.just(restApiResult);
        } catch (Exception e) {
            populateErrorDetails(restApiResult, e);
            return Mono.just(restApiResult);
        }
    }

    /**
     * This Method should be used to make an HTTP PUT call and shall deserialize the result to whatever class passed in <T>
     *
     * @param url         url of resources to PUT
     * @param objectToPut Type to deserialize list to
     * @return APIResult with deserialized value of response
     */
    @SuppressWarnings("unchecked")
    public <T> Mono<RestApiResult<T>> putResourceAsync(String url, T objectToPut) {
        RestApiResult<T> restApiResult = new RestApiResult<>();
        try {
            String postData = mapper.writeValueAsString(objectToPut);
            ByteBuf postBodyByteBuf = Unpooled.copiedBuffer(postData.getBytes());
            httpClient.put()
                    .uri(url)
                    .send(Mono.just(postBodyByteBuf))
                    .responseSingle((httpClientResponse, byteBufMono) -> {
                        //Populate Request metadata
                        restApiResult.setRequestPath(httpClientResponse.fullPath());
                        restApiResult.setHeaders(httpClientResponse.requestHeaders());
                        restApiResult.setHttpMethod(httpClientResponse.method());
                        restApiResult.setClientResponse(httpClientResponse);

                        //Determine if success
                        int responseStatus = httpClientResponse.status().code();

                        //If success, emit response as string to reactive flow
                        if (responseStatus >= 200 && responseStatus < 300) {
                            restApiResult.setSuccess(true);
                            return byteBufMono.asString();
                        }

                        //If error status code, populate ErrorResponse with appropriate HTTP level details and emit empty string
                        populateHttpErrorDetails(restApiResult, responseStatus, httpClientResponse);
                        return Mono.just("");
                    })
                    .log(null, Level.INFO, SignalType.ON_NEXT) // log when data comes through pipeline
                    .filter(s -> !s.isBlank())
                    .flatMap(s -> {
                        try {
                            //Attempt to deserialize and return ApiResult
                            restApiResult.setSuccessResult(deserialize(s.getBytes(StandardCharsets.UTF_8), (Class<T>) (objectToPut.getClass())));
                            return Mono.just(restApiResult);
                        } catch (Exception e) {
                            e.printStackTrace();
                            populateErrorDetails(restApiResult, e);
                            return Mono.error(e);
                        }
                    });
            return Mono.just(restApiResult);
        } catch (Exception e) {
            populateErrorDetails(restApiResult, e);
            return Mono.just(restApiResult);
        }
    }

    /**
     * This Method should be used to make an HTTP DELETE call and shall return an empty successResult <T>
     * <p>
     *
     * @param url url of resources to delete
     * @return APIResult with empty successResult
     */
    @SuppressWarnings("unchecked")
    public <T> Mono<RestApiResult<T>> deleteResourceAsync(String url) {
        RestApiResult<T> restApiResult = new RestApiResult<>();
        try {
            httpClient.delete()
                    .uri(url)
                    .responseSingle((httpClientResponse, byteBufMono) -> {
                        //Populate Request metadata
                        restApiResult.setRequestPath(httpClientResponse.fullPath());
                        restApiResult.setHeaders(httpClientResponse.requestHeaders());
                        restApiResult.setHttpMethod(httpClientResponse.method());
                        restApiResult.setClientResponse(httpClientResponse);

                        //Determine if success
                        int responseStatus = httpClientResponse.status().code();

                        //If success, emit response as string to reactive flow
                        if (responseStatus == 200 || responseStatus == 204) {
                            restApiResult.setSuccess(true);
                            return byteBufMono.asString();
                        }

                        //If error status code, populate ErrorResponse with appropriate HTTP level details and emit empty string
                        populateHttpErrorDetails(restApiResult, responseStatus, httpClientResponse);
                        return Mono.just("");
                    })
                    .log(null, Level.INFO, SignalType.ON_NEXT) // log when data comes through pipeline
                    .filter(s -> !s.isBlank())
                    .flatMap(s -> {
                        try {
                            //Attempt to deserialize and return ApiResult
                            restApiResult.setSuccessResult(null);
                            return Mono.just(restApiResult);
                        } catch (Exception e) {
                            e.printStackTrace();
                            populateErrorDetails(restApiResult, e);
                            return Mono.error(e);
                        }
                    });
            return Mono.just(restApiResult);
        } catch (Exception e) {
            populateErrorDetails(restApiResult, e);
            return Mono.just(restApiResult);
        }
    }

    /**
     * This method allows for the asynchronous deserialization of a byte[] to whatever Type provided
     *
     * @param jsonBytes  byte array to feed into async parser
     * @param returnType type to deserialize
     * @return deserialized value
     * @throws Exception throws exception if deserialization fails
     */
    private static <T> T deserialize(byte[] jsonBytes, Class<T> returnType) throws Exception {
        //Get Nonblocking Parser
        JsonParser asyncParser = mapper.getFactory().createNonBlockingByteArrayParser();

        //Feed response bytes into nonblocking feeder
        ByteArrayFeeder feeder = (ByteArrayFeeder) asyncParser.getNonBlockingInputFeeder();
        feeder.feedInput(jsonBytes, 0, jsonBytes.length);
        feeder.endOfInput();

        //Deserialize value
        return mapper.readValue(asyncParser, returnType);
    }

    /**
     * This method allows for the asynchronous deserialization of a byte[] to a list of whatever Type provided
     *
     * @param jsonBytes byte array to feed into async parser
     * @param returnType type to deserialize
     * @return list of deserialized values
     * @throws Exception throws exception if deserialization fails
     */
    private static <T> List<T> deserializeToList(byte[] jsonBytes, Class<T> returnType) throws Exception {
        //Get Nonblocking Parser
        JsonParser asyncParser = mapper.getFactory().createNonBlockingByteArrayParser();

        //Feed response bytes into nonblocking feeder
        ByteArrayFeeder feeder = ((ByteArrayFeeder) asyncParser.getNonBlockingInputFeeder());
        feeder.feedInput(jsonBytes, 0, jsonBytes.length);
        feeder.endOfInput();

        //Deserialize values to List
        CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, returnType);
        return mapper.readValue(jsonBytes, type);
    }

    private static <T> void populateHttpErrorDetails(RestApiResult<T> restApiResult, int responseStatus, HttpClientResponse httpClientResponse) {
        ErrorResponse errorResponse = restApiResult.getErrorResponse();
        if (errorResponse == null) {
            errorResponse = new ErrorResponse();
        }
        errorResponse.setHttpStatus(HttpStatusClass.valueOf(responseStatus));
        errorResponse.setClientResponse(httpClientResponse);

        restApiResult.setSuccess(false);

    }

    private static <T> void populateErrorDetails(RestApiResult<T> restApiResult, Exception e) {
        ErrorResponse errorResponse = restApiResult.getErrorResponse();
        if (errorResponse == null) {
            errorResponse = new ErrorResponse();
        }

        errorResponse.setErrorDescription(e.getMessage());
        errorResponse.setException(e);
        restApiResult.setErrorResponse(errorResponse);
        restApiResult.setSuccess(false);
    }
}
