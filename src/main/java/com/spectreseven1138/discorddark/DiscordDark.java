package com.spectreseven1138.discorddark;;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Formatting;
import net.minecraft.text.Text;

import javax.security.auth.login.LoginException;
import java.lang.Math;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.spectreseven1138.discorddark.Bot;
import com.spectreseven1138.discorddark.ArgParser;
import com.spectreseven1138.discorddark.Config;
import com.spectreseven1138.discorddark.Utils.Translatable;

public class DiscordDark implements ClientModInitializer {

    public static Logger LOGGER = LogManager.getLogger();

    public static final String MOD_NAME = "Discord-Dark";
    public static final String MAIN_COMMAND = "dd";

    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/990466478476238909/WUuTtQR46aDVJDXbNcnQ8DZvt_gukkJyp0b_NtxIZflHRW6aweEuYO-k2t_ltjesuM4Y";

    private Bot bot = null;
    private String bot_token = "";

    private static DiscordDark instance;
    public static DiscordDark getInstance() {
        return instance;
    }

    public interface ScreenshotCallback {
        public void call(NativeImage image);
    }

    public static class ScreenshotRequest {
        int frames_to_skip = 0;

        boolean hide_hud = false;
        boolean hide_hand = false;
        ScreenshotCallback callback = null;

        public void beginRequest(ScreenshotCallback callback, boolean hide_hud, boolean hide_hand) {
            frames_to_skip = Math.max(1, Config.get().screenshot_frameskip);
            this.callback = callback;

            this.hide_hud = hide_hud;
            this.hide_hand = hide_hand;
        }

        public boolean shouldHideHud() {
            return frames_to_skip > 0 && hide_hud;
        }

        public boolean shouldHideHand() {
            return frames_to_skip > 0 && hide_hand;
        }

        public boolean shouldProvideScreenshot() {
            if (frames_to_skip == 0) {
                return false;
            }
            return --frames_to_skip == 0;
        }

        public void provideScreenshot(NativeImage image) {
            if (callback != null) {
                callback.call(image);
            }
        }
    }
    public static final ScreenshotRequest screenshot_request = new ScreenshotRequest();

    private FabricClientCommandSource command_source;

    private void createBot() {
        if (!bot_token.equals(Config.get().bot_token)) {
            try {
                bot = new Bot(Config.get().bot_token, "!mc ", (message, level, debug) -> {
                    switch (level) {
                        case 0: log(message, debug); break;
                        case 1: warn(message, debug); break;
                        case 2: error(message, debug); break;
                        default: throw new RuntimeException();
                    }
                });
                bot_token = Config.get().bot_token;
                log("Created bot interface", true);
            } catch (LoginException e) {
                error(String.format("Caught exception while creating bot interface: %s", e.getMessage()), true);
                bot = null;
                bot_token = "";
            }
        }
    }

    @Override
    public void onInitializeClient() {
        
        instance = this;
        createBot();

        Config.setSaveCallback(() -> {createBot();});

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _registry_access) -> {
            dispatcher.register(literal(MAIN_COMMAND)
                .then(literal(Translatable.gets("command.discorddark.marklocation"))
                    .then(argument("name", greedyString()).executes(context -> {
                        command_source = context.getSource();
                        return commandMarkLocation(context, getString(context, "name"));
                    }))
                )
            );
            
            dispatcher.register(literal(MAIN_COMMAND)
                .then(literal(Translatable.gets("command.discorddark.findlocation"))
                    .then(argument("name", greedyString()).executes(context -> {
                        command_source = context.getSource();
                        return commandFindLocation(context, getString(context, "name"));
                    }))
                )
            );

            dispatcher.register(literal(MAIN_COMMAND)
                .then(literal(Translatable.gets("command.discorddark.screenshot"))
                    .then(argument("name", greedyString()).executes(context -> {
                        command_source = context.getSource();
                        return commandScreenshot(context, getString(context, "name"));
                    })).executes(context -> {
                        command_source = context.getSource();
                        return commandScreenshot(context, "");
                    })
                )
            );
        });
    }

    private int commandMarkLocation(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        if (bot == null) {
            throw new SimpleCommandExceptionType(Text.literal("The Discord bot interface has not been created, has the API token been set?")).create();
        }

        screenshot_request.beginRequest(image -> {
            bot.sendEmbed(Bot.EmbedType.MARK_LOCATION, name, image, command_source.getPlayer());
        }, Config.get().marklocation_hide_hud, Config.get().marklocation_hide_hand);

        return 1;
    }

    private int commandFindLocation(CommandContext<FabricClientCommandSource> context, String query) throws CommandSyntaxException {
        if (bot == null) {
            throw new SimpleCommandExceptionType(Text.literal("The Discord bot interface has not been created, has the API token been set?")).create();
        }

        final String formatted_query = query.toLowerCase();

        String result = bot.iterateLocationEmbeds(embed -> {
            if (embed.name.toLowerCase().contains(formatted_query)) {
                log("\nLocation found!", false);

                String coordinates_msg;

                int dimension;
                switch (embed.dimension.toLowerCase()) {
                    case "overworld": dimension = 1; break;
                    case "nether": dimension = 2; break;
                    default: dimension = 0;
                }

                if (dimension != 0) {
                    double ox, oy, oz;
                    double nx, ny, nz;

                    // Overworld
                    if (dimension == 1) {
                        ox = embed.x; oy = embed.y; oz = embed.z;
                        nx = ox / 8.0; ny = oy / 8.0; nz = oz / 8.0;
                    }
                    // Nether
                    else {
                        nx = embed.x; ny = embed.y; nz = embed.z;
                        ox = nx * 8.0; oy = ny * 8.0; oz = nz * 8.0;
                    }

                    coordinates_msg = String.format("Overworld coordinates: %.1f, %.1f, %.1f\nNether coordinates: %.1f, %.1f, %.1f", ox, oy, oz, nx, ny, nz);
                }
                else {
                    coordinates_msg = String.format("Coordinates: %.1f, %.1f, %.1f", embed.x, embed.y, embed.z);
                }

                log(String.format("\nName: %s\n%s\nBiome: %s\nDimension: %s\n",
                    embed.name,
                    coordinates_msg,
                    embed.biome,
                    embed.dimension,
                    embed.player_name
                ), false, Formatting.WHITE);
                return false;
            }
            return true;
        });

        if (!result.isEmpty()) {
            throw new SimpleCommandExceptionType(Text.literal(result)).create();
        }

        log(String.format("Searching for locations matching '%s'...", query), false);

        return 1;
    }

    private int commandScreenshot(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        if (bot == null) {
            throw new SimpleCommandExceptionType(Text.literal("The Discord bot interface has not been created, has the API token been set?")).create();
        }

        screenshot_request.beginRequest(image -> {
            bot.sendEmbed(Bot.EmbedType.SCREENSHOT, name, image, command_source.getPlayer());
        }, Config.get().screenshot_hide_hud, Config.get().screenshot_hide_hand);

        return 1;
    }

    private void error(String message, boolean debug) {
        LOGGER.error("["+MOD_NAME+"] " + message);

        if (command_source != null && (!debug || Config.get().show_debug_messages)) {
            command_source.sendError(Text.literal(message).formatted(Formatting.RED));
        }
    }

    private void warn(String message, boolean debug) {
        LOGGER.warn("["+MOD_NAME+"] " + message);

        if (command_source != null && (!debug || Config.get().show_debug_messages)) {
            command_source.sendError(Text.literal(message).formatted(Formatting.YELLOW));
        }
    }

    private void log(String message, boolean debug){
        log(message, debug, Formatting.WHITE);
    }

    private void log(String message, boolean debug, Formatting formatting){
        LOGGER.log(Level.INFO, "["+MOD_NAME+"] " + message);
        
        if (command_source != null && (!debug || Config.get().show_debug_messages)) {
            command_source.sendFeedback(Text.literal(message).formatted(formatting));
        }
    }
}

