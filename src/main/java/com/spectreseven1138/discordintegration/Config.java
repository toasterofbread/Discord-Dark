package com.spectreseven1138.discordintegration;

import net.minecraft.util.Identifier;
import net.minecraft.client.gui.screen.Screen;

import net.fabricmc.loader.api.FabricLoader;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;

import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import com.spectreseven1138.discordintegration.Translateable;

class ConfigFormat {
    String bot_token = "";
    boolean show_debug_messages = false;
    boolean play_sounds = true;

    long guild_id = 0L;
    long location_channel_id = 0L;
    long screenshot_channel_id = 0L;
    long backend_channel_id = 0L;

    ConfigFormat() {}
}

public class Config {

    public interface SaveCallback {
        public void call();
    }
    private static SaveCallback save_callback = null;

    public static void setSaveCallback(SaveCallback callback) {
        save_callback = callback;
    }

    private static ConfigFormat config = null;

    public static ConfigFormat get() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("discordintegration.json");
    }

    public static void loadConfig() {
        Path path = getConfigPath();
        if (Files.exists(path)) {
            try (InputStream stream = Files.newInputStream(path)) {
                Gson g = new Gson();
                config = g.fromJson(IOUtils.toString(stream, StandardCharsets.UTF_8), ConfigFormat.class);
            }
            catch (IOException e) {
                throw new JsonParseException(e);
            }
        } else {
            config = new ConfigFormat();
        }
    }

    public static void saveConfig() {
        if (config == null) {
            loadConfig();
        }
        else {
            if (save_callback != null) {
                save_callback.call();
            }
        }

        Path path = getConfigPath();

        try {
            Files.createDirectories(path.getParent());
            Gson g = new Gson();
            Files.write(path, g.toJson(config).getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e) {
            throw new JsonParseException(e);
        }
    }

    public static Screen buildMenu(Screen parent) {
        loadConfig();

        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Translateable.get("title.discordintegration.config"));
        builder.setDefaultBackgroundTexture(new Identifier("minecraft:textures/block/sculk_catalyst_top.png"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Translateable.get("category.discordintegration.general"));
        general.addEntry(entryBuilder.startStrField(Translateable.get("config.discordintegration.bot_token"), config.bot_token).setTooltip(Translateable.get("config.discordintegration.bot_token.tooltip")).setDefaultValue("").setSaveConsumer(value -> {config.bot_token = value; saveConfig();}).build());
        general.addEntry(entryBuilder.startBooleanToggle(Translateable.get("config.discordintegration.play_sounds"), config.play_sounds).setTooltip(Translateable.get("config.discordintegration.play_sounds.tooltip")).setDefaultValue(true).setSaveConsumer(value -> {config.play_sounds = value; saveConfig();}).build());
        general.addEntry(entryBuilder.startBooleanToggle(Translateable.get("config.discordintegration.show_debug_messages"), config.show_debug_messages).setTooltip(Translateable.get("config.discordintegration.show_debug_messages.tooltip")).setDefaultValue(false).setSaveConsumer(value -> {config.show_debug_messages = value; saveConfig();}).build());

        ConfigCategory server = builder.getOrCreateCategory(Translateable.get("category.discordintegration.server"));
        server.addEntry(entryBuilder.startLongField(Translateable.get("config.discordintegration.guild_id"), config.guild_id).setTooltip(Translateable.get("config.discordintegration.guild_id.tooltip")).setDefaultValue(0L).setSaveConsumer(value -> {config.guild_id = value; saveConfig();}).build());
        server.addEntry(entryBuilder.startLongField(Translateable.get("config.discordintegration.location_channel_id"), config.location_channel_id).setTooltip(Translateable.get("config.discordintegration.location_channel_id.tooltip")).setDefaultValue(0L).setSaveConsumer(value -> {config.location_channel_id = value; saveConfig();}).build());
        server.addEntry(entryBuilder.startLongField(Translateable.get("config.discordintegration.screenshot_channel_id"), config.screenshot_channel_id).setTooltip(Translateable.get("config.discordintegration.screenshot_channel_id.tooltip")).setDefaultValue(0L).setSaveConsumer(value -> {config.screenshot_channel_id = value; saveConfig();}).build());
        server.addEntry(entryBuilder.startLongField(Translateable.get("config.discordintegration.backend_channel_id"), config.backend_channel_id).setTooltip(Translateable.get("config.discordintegration.backend_channel_id.tooltip")).setDefaultValue(0L).setSaveConsumer(value -> {config.backend_channel_id = value; saveConfig();}).build());

        return builder.build();
    }

}
