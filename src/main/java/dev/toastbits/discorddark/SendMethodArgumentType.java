package dev.toastbits.discorddark;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.CompletableFuture;

public final class SendMethodArgumentType implements ArgumentType<SendMethod> {

    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();

    @Override
    public SendMethod parse(StringReader reader) throws CommandSyntaxException {

        if (Config.get().send_methods.isEmpty()) {
            throw new SimpleCommandExceptionType(Utils.Translatable.get("error.discorddark.no_send_methods")).createWithContext(reader);
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
        
        throw new SimpleCommandExceptionType(Utils.Translatable.get("error.discorddark.invalid_send_method")).createWithContext(reader);
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