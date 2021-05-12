# ReactiveHttpClient
This Repo is for a Reactive HTTP Client with Non-Blocking JSON Deserialization

##**_Retrieve single REST Resource_**

Mono<RestApiResult<String>> stringResultMono = RestClientManager.getResourceAsync("http://localhost:8080/strings/1",
String.class);

. . Do some work . .

RestApiResult<String> stringResult = stringResultMono.block(); //Or any other Mono operator

if (stringResult.isSuccess()) { String s = stringResult.getSuccessResult(); }

##**_Retrieve List of REST Resources_**

Mono<RestApiResult<String>> stringResultMono = RestClientManager.getResourcesAsync("http://localhost:8080/strings",
String.class);

. . Do some work . .

RestApiResult<String> stringResult = stringResultMono.block(); //Or any other Mono operator

if (stringResult.isSuccess()) { List<String> s = stringResult.getSuccessResults(); }

##**_Utilizing ErrorResponse_**

Mono<RestApiResult<String>> stringResultMono = RestClientManager.getResourceAsync("http://localhost:8080/strings/1",
String.class);

. . Do some work . HTTP ERROR OCCURS .

RestApiResult<String> stringResult = stringResultMono.block(); //Or any other Mono operator

if (!stringResult.isSuccess()) { ErrorResponse errorResponse = stringResult.getErrorResponse();
errorResponse.getHttpStatus(); // gives HTTP Status errorResponse.getHttpClientResponse(); //gives response to consume
and gather more details }


