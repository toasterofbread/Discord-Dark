package com.spectreseven1138.discorddark;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import com.google.common.collect.Lists;
import java.util.concurrent.CompletableFuture;

import com.spectreseven1138.discorddark.Config;
import com.spectreseven1138.discorddark.SendMethod;
import com.spectreseven1138.discorddark.Utils.Translatable;

public final class SendMethodArgumentType implements ArgumentType<SendMethod> {

    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();

    @Override
    public SendMethod parse(StringReader reader) throws CommandSyntaxException {

        if (Config.get().send_methods.isEmpty()) {
            throw new SimpleCommandExceptionType(Translatable.get("error.discorddark.no_send_methods")).createWithContext(reader);
        }

        int beginning = reader.getCursor();
        if (!reader.canRead()) {
            reader.skip();
        }

        while (reader.canRead() && reader.peek() != ' ') { 
            reader.skip();
        }

        String method_identifier = reader.getString().substring(beginning, reader.getCursor());
        
        for (SendMethod method : Config.get().send_methods) {
            if (method.methodMatches(method_identifier)) {
                return method;
            }
        }
        
        throw new SimpleCommandExceptionType(Translatable.get("error.discorddark.invalid_send_method")).createWithContext(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        for (SendMethod method : Config.get().send_methods) {
            builder.suggest(method.identifier);
        }
        return builder.buildFuture();
    } 

    public static SendMethodArgumentType sendMethod() {
        return new SendMethodArgumentType();
    }
 
    public static <S> SendMethod getSendMethod(CommandContext<S> context, String name) {
        return context.getArgument(name, SendMethod.class);
    }
 
}