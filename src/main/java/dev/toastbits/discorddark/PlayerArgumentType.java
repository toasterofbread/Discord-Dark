package dev.toastbits.discorddark;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

import static dev.toastbits.discorddark.DiscordDark.log;

public final class PlayerArgumentType implements ArgumentType<AbstractClientPlayerEntity> {

    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();

    @Override
    public AbstractClientPlayerEntity parse(StringReader reader) throws CommandSyntaxException {

        int beginning = reader.getCursor();
        if (!reader.canRead()) {
            reader.skip();
        }

        while (reader.canRead() && reader.peek() != ' ') { 
            reader.skip();
        }

        String player_string = reader.getString().substring(beginning, reader.getCursor());
        
        boolean no_players = true;
        int i = 0;

        for (AbstractClientPlayerEntity player : CLIENT.player.clientWorld.getPlayers()) {
            if (player == CLIENT.player) {
                continue;
            }

            no_players = false;

            if (player.getName().getString().equals(player_string)) {
                return player;
            }
        }

        Text message;

        if (no_players) {
            message = Utils.Translatable.get("error.discorddark.no_players");
        }
        else {
            message = Text.literal(String.format(Utils.Translatable.gets("error.discorddark.invalid_player"), player_string));
        }

        throw new SimpleCommandExceptionType(message).createWithContext(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        for (AbstractClientPlayerEntity player : CLIENT.player.clientWorld.getPlayers()) {
            if (player == CLIENT.player) {
                continue;
            }
            builder.suggest(player.getName().getString());
        }
        return builder.buildFuture();
    } 

    public static PlayerArgumentType player() {
        return new PlayerArgumentType();
    }
 
    public static <S> AbstractClientPlayerEntity getPlayer(CommandContext<S> context, String name) {
        return context.getArgument(name, AbstractClientPlayerEntity.class);
    }
 
}