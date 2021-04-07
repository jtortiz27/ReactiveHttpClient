package com.ortiz.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.ortiz.model.RestApiResult;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.netty.http.client.HttpClient;

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
     * @return
     */
    public <T> Mono<RestApiResult<T>> getResourceAsync(String url, final Class<T> returnType) {
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

                        //If error status code, throw error
                        return Mono.error(new Exception("Received Error Status Code"));

                    }))
                    .log(null, Level.INFO, SignalType.ON_NEXT) // log when data comes through pipeline
                    .flatMap(s -> {
                        try {
                            //Attempt to deserialize and return ApiResult
                            restApiResult.setSuccessResult(deserialize(s.getBytes(StandardCharsets.UTF_8), returnType));
                            return Mono.just(restApiResult);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return Mono.error(e);
                        }
                    }).single();
        } catch (Exception e) {
            return Mono.error(new Exception("Excpetion occured while retrieving resource"));
        }
    }

    /**
     * This Method should be used to make an HTTP GET call and shall deserialize the result to whatever class passed in <T>
     * <p>
     * Note: This is for retrieving a list of resources and upon success, the deserialized result will be available in APIResult.getSuccessResults()
     *
     * @param url  url of resources to retrieve
     * @param returnType Type to deserialize list to
     * @return
     */
    public <T> Mono<RestApiResult<T>> getResourcesAsync(String url, final Class<T> returnType) {
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

                        //If error status code, throw error
                        return Mono.error(new Exception("Received Error Status Code"));

                    }))
                    .log(null, Level.INFO, SignalType.ON_NEXT) // log when data comes through pipeline
                    .flatMap(s -> {
                        try {
                            //Attempt to deserialize records and return ApiResult
                            restApiResult.setSuccessResults(deserializeToList(s.getBytes(StandardCharsets.UTF_8), returnType));
                            return Mono.just(restApiResult);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return Mono.error(e);
                        }
                    });
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    /**
     * This method allows for the asynchronous deserialization of a byte[] to whatever Type provided
     *
     * @param jsonBytes
     * @param returnType
     * @param <T>
     * @return
     * @throws Exception
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
     * @param jsonBytes
     * @param returnType
     * @param <T>
     * @return
     * @throws Exception
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
}
