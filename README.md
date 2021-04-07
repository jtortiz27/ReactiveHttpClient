# ReactiveHttpClient
This Repo is for a Reactive HTTP Client with Non-Blocking JSON Deserialization



**_Retrieve single REST Resource_**


Mono<ApiResult<String>> stringResultMono = RestClientManager.getResourceAsync("http://localhost:8080/strings/1", String.class);

.
. Do some work
.

ApiResult<String> stringResult = stringResultMono.block(); //Or any other Mono operator

if (stringResult.isSuccess()) {
    String s = stringResult.getSuccessResult();
}






**_Retrieve array of REST Resources_**


Mono<ApiResult<String[]>> stringResultMono = RestClientManager.getResourcesAsync("http://localhost:8080/strings", String[].class);

.
. Do some work
.

ApiResult<String[]> stringResult = stringResultMono.block(); //Or any other Mono operator

if (stringResult.isSuccess()) {
    String[] s = stringResult.getSuccessResults();
}
