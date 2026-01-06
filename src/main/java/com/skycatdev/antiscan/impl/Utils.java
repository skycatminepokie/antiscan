package com.skycatdev.antiscan.impl;

import com.mojang.serialization.DataResult;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Utils {

    public static <T> DataResult<HttpResponse<T>> httpRequest(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
        // https://curlconverter.com/java/
        HttpResponse<T> response;
        //? if >=1.20.5
        try (HttpClient client = HttpClient.newHttpClient()) {
            //? if <1.20.5
            /*HttpClient client = HttpClient.newHttpClient();*/
            try {
                response = client.send(request, bodyHandler);
            } catch (IOException | InterruptedException e) {
                return DataResult.error(() -> "Failed with error: " + e);
            }
            //? if >=1.20.5
        }
        if (response != null) {
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return DataResult.success(response);
            } else {
                return DataResult.error(() -> "Failed with non-2xx status code", response);
            }
        } else {
            return DataResult.error(() -> "Failed to send request - response returned from client was null.");
        }
    }

}
