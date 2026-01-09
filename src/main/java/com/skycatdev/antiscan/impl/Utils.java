package com.skycatdev.antiscan.impl;

//? if >=1.21.5
import com.google.gson.FormattingStyle;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import java.io.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Utils {

    public static <T> DataResult<HttpResponse<T>> sendHttpRequest(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
        // https://curlconverter.com/java/
        HttpResponse<T> response;
        //? if >=1.20.5
        try (HttpClient client = HttpClient.newHttpClient()) {
            //? if <1.20.5
            //HttpClient client = HttpClient.newHttpClient();
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

    public static <T> void saveToFile(T t, File file, Codec<T> codec) throws IOException {
        if (!file.exists()) {
            if (file.isDirectory() || !file.createNewFile()) {
                throw new FileNotFoundException();
            }
        }
        JsonElement json;
        //? if >=1.20.5
        json = codec.encode(t, JsonOps.INSTANCE, JsonOps.INSTANCE.empty()).getOrThrow(IOException::new);
        //? if <1.20.5 {
        /*try {
            json = codec.encode(t, JsonOps.INSTANCE, JsonOps.INSTANCE.empty()).getOrThrow(false, str -> {});
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
        *///?}
        try (JsonWriter writer = new JsonWriter(new PrintWriter(file))) {
            //? if >=1.21.5
            writer.setFormattingStyle(FormattingStyle.PRETTY);
            //? if <1.21.5
            //writer.setIndent("  ");
            Streams.write(json, writer);
        }
    }

    public static <T> T loadFromFile(File file, Codec<T> codec) throws IOException {
        try (JsonReader reader = new JsonReader(new FileReader(file))) {
            //? if >=1.20.5
            return codec.decode(JsonOps.INSTANCE, Streams.parse(reader)).getOrThrow().getFirst();
            //? if <1.20.5 {
            /*try {
                return codec.decode(JsonOps.INSTANCE, Streams.parse(reader)).getOrThrow(false, str -> {}).getFirst();
            } catch (RuntimeException e) {
                throw new IOException(e);
            }
            *///?}
        }
    }

}
