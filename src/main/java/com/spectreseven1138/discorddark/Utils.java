package com.spectreseven1138.discorddark;

import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableTextContent;

public class Utils {
    public static class Translatable {
        public static MutableText get(String key) {
            return MutableText.of(new TranslatableTextContent(key));
        }
        public static String gets(String key) {
            return MutableText.of(new TranslatableTextContent(key)).getString();
        }
    }

    public interface ValueCallback<T, R> {
        public R call(T value);
    }

    public interface ResultCallback<T> {
        public void call(boolean error, T result);
    }
}
