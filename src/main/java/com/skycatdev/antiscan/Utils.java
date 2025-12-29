package com.skycatdev.antiscan;

//? if >=1.21.5

import com.google.gson.FormattingStyle;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;

import java.io.*;

public class Utils {

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
            /*writer.setIndent("  ");*/
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

    public static Component textOf(String text) {
        return Component.literal(text);
    }

    public static Component translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }
}
