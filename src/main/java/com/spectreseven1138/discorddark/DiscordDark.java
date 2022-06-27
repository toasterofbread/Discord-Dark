package com.spectreseven1138.discorddark;;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Formatting;
import net.minecraft.text.Text;

import javax.security.auth.login.LoginException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.spectreseven1138.discorddark.Bot;
import com.spectreseven1138.discorddark.ArgParser;
import com.spectreseven1138.discorddark.Config;
import com.spectreseven1138.discorddark.Translatable;

public class DiscordDark implements ClientModInitializer {

    public static Logger LOGGER = LogManager.getLogger();

    public static final String MOD_ID = "discorddark";
    public static final String MOD_NAME = "Discord Location Marker";

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
        boolean screenshot_requested = false;
        boolean hide_hud = false;
        boolean hide_hand = false;
        ScreenshotCallback callback = null;

        public void beginRequest(ScreenshotCallback callback, boolean hide_hud, boolean hide_hand) {
            screenshot_requested = true;
            this.callback = callback;

            this.hide_hud = hide_hud;
            this.hide_hand = hide_hand;
        }

        public boolean shouldHideHud() {
            if (hide_hud) {
                hide_hud = false;
                return true;
            }
            return false;
        }

        public boolean shouldHideHand() {
            if (hide_hand) {
                hide_hand = false;
                return true;
            }
            return false;
        }

        boolean hasDecorations() {
            return hide_hand || hide_hud;
        }

        public boolean shouldProvideScreenshot() {
            return screenshot_requested && !hasDecorations();
        }

        public void provideScreenshot(NativeImage image) {
            screenshot_requested = false;

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
            dispatcher.register(ClientCommandManager.literal(Translatable.get("command.discorddark.marklocation").getString()).then(ClientCommandManager.argument("name", word()).executes(context -> {
                command_source = context.getSource();
                if (bot == null) {
                    error("The Discord bot interface has not been created, has the API token been set?", false);
                    return 1;
                }

                screenshot_request.beginRequest(image -> {
                    bot.sendEmbed(Bot.EmbedType.MARK_LOCATION, context.getArgument("name", String.class), image, command_source.getPlayer());
                }, Config.get().marklocation_hide_hud, Config.get().marklocation_hide_hand);

                return 1;
            })));
        });
        log("Registered command (name: " + Translatable.get("command.discorddark.marklocation").getString() + ")", true);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _registry_access) -> {
            dispatcher.register(ClientCommandManager.literal(Translatable.get("command.discorddark.screenshot").getString()).then(ClientCommandManager.argument("name", word()).executes(context -> {
                command_source = context.getSource();
                if (bot == null) {
                    error("The Discord bot interface has not been created, has the API token been set?", false);
                    return 1;
                }

                screenshot_request.beginRequest(image -> {
                    bot.sendEmbed(Bot.EmbedType.SCREENSHOT, context.getArgument("name", String.class), image, command_source.getPlayer());
                }, Config.get().screenshot_hide_hud, Config.get().screenshot_hide_hand);

                return 1;
            })));
        });
        log("Registered command (name: " + Translatable.get("command.discorddark.screenshot").getString() + ")", true);
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
        LOGGER.log(Level.INFO, "["+MOD_NAME+"] " + message);
        
        if (command_source != null && (!debug || Config.get().show_debug_messages)) {
            command_source.sendFeedback(Text.literal(message).formatted(Formatting.WHITE));
        }
    }
}

