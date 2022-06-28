package com.spectreseven1138.discorddark;

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

import com.spectreseven1138.discorddark.Translatable;

class ConfigFormat {
    String bot_token = "";
    boolean play_sounds = true;

    boolean marklocation_hide_hud = true;
    boolean marklocation_hide_hand = true;

    boolean screenshot_hide_hud = false;
    boolean screenshot_hide_hand = false;

    long guild_id = 0L;
    long location_channel_id = 0L;
    long screenshot_channel_id = 0L;
    long backend_channel_id = 0L;
    
    boolean show_debug_messages = false;
    int screenshot_frameskip = 2;

    ConfigFormat() {}
}

public class Config {

    public static Screen buildMenu(Screen parent) {
        loadConfig();

        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Translatable.get("title.discorddark.config"));
        builder.setDefaultBackgroundTexture(new Identifier("minecraft:textures/block/sculk_catalyst_top.png"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Translatable.get("category.discorddark.general"));
        general.addEntry(entryBuilder.startStrField(Translatable.get("config.discorddark.bot_token"), config.bot_token).setTooltip(Translatable.get("config.discorddark.bot_token.tooltip")).setDefaultValue("").setSaveConsumer(value -> {config.bot_token = value; saveConfig();}).build());
        general.addEntry(entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.play_sounds"), config.play_sounds).setTooltip(Translatable.get("config.discorddark.play_sounds.tooltip")).setDefaultValue(true).setSaveConsumer(value -> {config.play_sounds = value; saveConfig();}).build());

        ConfigCategory server = builder.getOrCreateCategory(Translatable.get("category.discorddark.server"));
        server.addEntry(entryBuilder.startLongField(Translatable.get("config.discorddark.guild_id"), config.guild_id).setTooltip(Translatable.get("config.discorddark.guild_id.tooltip")).setDefaultValue(0L).setSaveConsumer(value -> {config.guild_id = value; saveConfig();}).build());
        server.addEntry(entryBuilder.startLongField(Translatable.get("config.discorddark.location_channel_id"), config.location_channel_id).setTooltip(Translatable.get("config.discorddark.location_channel_id.tooltip")).setDefaultValue(0L).setSaveConsumer(value -> {config.location_channel_id = value; saveConfig();}).build());
        server.addEntry(entryBuilder.startLongField(Translatable.get("config.discorddark.screenshot_channel_id"), config.screenshot_channel_id).setTooltip(Translatable.get("config.discorddark.screenshot_channel_id.tooltip")).setDefaultValue(0L).setSaveConsumer(value -> {config.screenshot_channel_id = value; saveConfig();}).build());
        server.addEntry(entryBuilder.startLongField(Translatable.get("config.discorddark.backend_channel_id"), config.backend_channel_id).setTooltip(Translatable.get("config.discorddark.backend_channel_id.tooltip")).setDefaultValue(0L).setSaveConsumer(value -> {config.backend_channel_id = value; saveConfig();}).build());

        ConfigCategory marklocation = builder.getOrCreateCategory(Translatable.get("category.discorddark.marklocation"));
        marklocation.addEntry(entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.marklocation_hide_hud"), config.marklocation_hide_hud).setTooltip(Translatable.get("config.discorddark.marklocation_hide_hud.tooltip")).setDefaultValue(true).setSaveConsumer(value -> {config.marklocation_hide_hud = value; saveConfig();}).build());
        marklocation.addEntry(entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.marklocation_hide_hand"), config.marklocation_hide_hand).setTooltip(Translatable.get("config.discorddark.marklocation_hide_hand.tooltip")).setDefaultValue(true).setSaveConsumer(value -> {config.marklocation_hide_hand = value; saveConfig();}).build());

        ConfigCategory screenshot = builder.getOrCreateCategory(Translatable.get("category.discorddark.screenshot"));
        screenshot.addEntry(entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.screenshot_hide_hud"), config.screenshot_hide_hud).setTooltip(Translatable.get("config.discorddark.screenshot_hide_hud.tooltip")).setDefaultValue(false).setSaveConsumer(value -> {config.screenshot_hide_hud = value; saveConfig();}).build());
        screenshot.addEntry(entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.screenshot_hide_hand"), config.screenshot_hide_hand).setTooltip(Translatable.get("config.discorddark.screenshot_hide_hand.tooltip")).setDefaultValue(false).setSaveConsumer(value -> {config.screenshot_hide_hand = value; saveConfig();}).build());
        
        ConfigCategory debug = builder.getOrCreateCategory(Translatable.get("category.discorddark.debug"));
        debug.addEntry(entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.show_debug_messages"), config.show_debug_messages).setTooltip(Translatable.get("config.discorddark.show_debug_messages.tooltip")).setDefaultValue(false).setSaveConsumer(value -> {config.show_debug_messages = value; saveConfig();}).build());
        debug.addEntry(entryBuilder.startIntSlider(Translatable.get("config.discorddark.screenshot_frameskip"), config.screenshot_frameskip, 1, 10).setTooltip(Translatable.get("config.discorddark.screenshot_frameskip.tooltip")).setDefaultValue(2).setSaveConsumer(value -> {config.screenshot_frameskip = value; saveConfig();}).build());

        return builder.build();
    }

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
        return FabricLoader.getInstance().getConfigDir().resolve("discorddark.json");
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
}
