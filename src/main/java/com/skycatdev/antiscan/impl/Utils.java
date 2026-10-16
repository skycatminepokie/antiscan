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
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class Utils {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(5)).build();

    /** Limit the download, not just the parser, when a source serves an oversized response. */
    public static HttpResponse.BodyHandler<String> boundedBody() {
        return info -> new HttpResponse.BodySubscriber<>() {
            final HttpResponse.BodySubscriber<String> delegate = HttpResponse.BodySubscribers.ofString(java.nio.charset.StandardCharsets.UTF_8);
            Flow.Subscription subscription;
            int bytes;
            public CompletionStage<String> getBody() { return delegate.getBody(); }
            public void onSubscribe(Flow.Subscription value) { subscription = value; delegate.onSubscribe(value); }
            public void onNext(List<ByteBuffer> buffers) {
                for (ByteBuffer buffer : buffers) {
                    bytes += buffer.remaining();
                    if (bytes > FeedLimits.MAX_FEED_BYTES) {
                        subscription.cancel();
                        delegate.onError(new IOException("HTTP response exceeds 8 MiB"));
                        return;
                    }
                }
                delegate.onNext(buffers);
            }
            public void onError(Throwable error) { delegate.onError(error); }
            public void onComplete() { delegate.onComplete(); }
        };
    }

    public static void atomicWrite(java.nio.file.Path path, String content) throws IOException {
        java.nio.file.Files.createDirectories(path.getParent());
        java.nio.file.Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        java.nio.file.Files.writeString(temp, content, java.nio.charset.StandardCharsets.UTF_8);
        try {
            java.nio.file.Files.move(temp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            java.nio.file.Files.move(temp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }


    public static <T> DataResult<HttpResponse<T>> sendHttpRequest(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
        HttpResponse<T> response;
        try {
            response = HTTP.send(request, bodyHandler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return DataResult.error(() -> "Interrupted HTTP request");
        } catch (IOException e) {
            return DataResult.error(() -> "HTTP request failed: " + e.getMessage());
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
        StringWriter output = new StringWriter();
        try (JsonWriter writer = new JsonWriter(output)) {
            //? if >=1.21.5
            writer.setFormattingStyle(FormattingStyle.PRETTY);
            //? if <1.21.5
            //writer.setIndent("  ");
            Streams.write(json, writer);
        }
        atomicWrite(file.toPath().toAbsolutePath(), output.toString());
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
