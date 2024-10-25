package dev.toastbits.discorddark;

import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableTextContent;

public class Utils {
    public static class Translatable {
        public static MutableText get(String key) {
            return MutableText.of(new TranslatableTextContent(key, null, new Object[]{}));
        }
        public static String gets(String key) {
            return MutableText.of(new TranslatableTextContent(key, null, new Object[]{})).getString();
        }
    }

    public static class Dimension {
        public static String fromReadable(String readable) {
            if (readable.equals(Translatable.gets("dimension.discorddark.overworld"))) {
                return "minecraft:overworld";
            }
            else if (readable.equals(Translatable.gets("dimension.discorddark.nether")))  {
                return "minecraft:the_nether";
            }
            else if (readable.equals(Translatable.gets("dimension.discorddark.end"))) {
                return "minecraft:the_end";
            }
            else {
                return readable;
            }
        }
        public static String toReadable(String dimension) {
            switch (dimension) {
                case "minecraft:overworld": return Translatable.gets("dimension.discorddark.overworld");
                case "minecraft:the_nether": return Translatable.gets("dimension.discorddark.nether");
                case "minecraft:the_end": return Translatable.gets("dimension.discorddark.end");
                default: return dimension;
            }
        }
    }

}
