package com.skycatdev.antiscan;

import net.minecraft.text.Text;

public class Utils {
    public static Text textOf(String text) {
        return Text.literal(text);
    }

    public static Text translatable(String key, Object... args) {
        return Text.translatable(key, args);
    }
}
