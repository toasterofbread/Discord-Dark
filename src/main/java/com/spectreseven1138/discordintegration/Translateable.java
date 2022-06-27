package com.spectreseven1138.discordintegration;

import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableTextContent;

public class Translateable {
    public static MutableText get(String key) {
        return MutableText.of(new TranslatableTextContent(key));
    }
}