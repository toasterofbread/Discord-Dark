package com.spectreseven1138.discordintegration;;

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

import com.spectreseven1138.discordintegration.Bot;
import com.spectreseven1138.discordintegration.ArgParser;
import com.spectreseven1138.discordintegration.Config;

public class DiscordIntegration implements ClientModInitializer {

    public static Logger LOGGER = LogManager.getLogger();

    public static final String MOD_ID = "discordintegration";
    public static final String MOD_NAME = "Discord Location Marker";

    private static final String COMMAND_NAME = "marklocation";
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/990466478476238909/WUuTtQR46aDVJDXbNcnQ8DZvt_gukkJyp0b_NtxIZflHRW6aweEuYO-k2t_ltjesuM4Y";

    private Bot bot = null;
    private String bot_token = "";

    private static DiscordIntegration instance;
    public static DiscordIntegration getInstance() {
        return instance;
    }

    public static int awaiting_screenshot = 0;
    private String mark_location_name;
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
            dispatcher.register(ClientCommandManager.literal(COMMAND_NAME).then(ClientCommandManager.argument("name", word()).executes(context -> {

                command_source = context.getSource();
                
                if (bot == null) {
                    error("The Discord bot interface has not been created, has the API token been set?", false);
                    return 1;
                }

                mark_location_name = context.getArgument("name", String.class);
                awaiting_screenshot = 1;

                return 1;
            })));
        });

        log("Registered command (name: " + COMMAND_NAME + ")", true);
    }

    public void provideScreenshot(NativeImage image) {
        awaiting_screenshot = 0;
        bot.markLocation(mark_location_name, image, command_source.getPlayer());
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

