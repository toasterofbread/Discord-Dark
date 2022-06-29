package com.spectreseven1138.discorddark;

import net.minecraft.util.Identifier;
import net.minecraft.client.gui.screen.Screen;
import static net.minecraft.text.Text.literal;
import net.minecraft.text.Text;

import net.fabricmc.loader.api.FabricLoader;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.impl.ConfigBuilderImpl;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.StringFieldBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;

import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.common.collect.Lists;

import com.spectreseven1138.discorddark.Utils.Translatable;
import com.spectreseven1138.discorddark.SendMethod;

public class Config {

    static class ConfigFormat {
        String bot_token = "";
        boolean play_sounds = true;

        long guild_id = 0L;
        long default_channel_id = 0L;
        long backend_channel_id = 0L;
        
        boolean show_debug_messages = false;
        int screenshot_frameskip = 2;

        List<SendMethod> send_methods = getDefaultMethodList();

        ConfigFormat() {}
    }

    public static Screen buildMenu(Screen parent) {
        loadConfig();

        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Translatable.get("title.discorddark.config"));
        builder.setDefaultBackgroundTexture(new Identifier("minecraft:textures/block/sculk_catalyst_top.png"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Translatable.get("category.discorddark.general"));
        general.addEntry(entryBuilder.startStrField(Translatable.get("config.discorddark.bot_token"), config.bot_token).setTooltip(Translatable.get("config.discorddark.bot_token.tooltip")).setDefaultValue("").setSaveConsumer(value -> {config.bot_token = value;}).build());
        general.addEntry(entryBuilder.startLongField(Translatable.get("config.discorddark.default_guild_id"), config.guild_id).setTooltip(Translatable.get("config.discorddark.default_guild_id.tooltip")).setDefaultValue(0L).setSaveConsumer(value -> {config.guild_id = value;}).build());

        // ConfigCategory server = builder.getOrCreateCategory(Translatable.get("category.discorddark.server"));

        // ConfigCategory marklocation = builder.getOrCreateCategory(Translatable.get("category.discorddark.marklocation"));
        // marklocation.addEntry(entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.marklocation_hide_hud"), config.marklocation_hide_hud).setTooltip(Translatable.get("config.discorddark.marklocation_hide_hud.tooltip")).setDefaultValue(true).setSaveConsumer(value -> {config.marklocation_hide_hud = value;}).build());
        // marklocation.addEntry(entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.marklocation_hide_hand"), config.marklocation_hide_hand).setTooltip(Translatable.get("config.discorddark.marklocation_hide_hand.tooltip")).setDefaultValue(true).setSaveConsumer(value -> {config.marklocation_hide_hand = value;}).build());

        // ConfigCategory screenshot = builder.getOrCreateCategory(Translatable.get("category.discorddark.screenshot"));
        // screenshot.addEntry(entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.screenshot_hide_hud"), config.screenshot_hide_hud).setTooltip(Translatable.get("config.discorddark.screenshot_hide_hud.tooltip")).setDefaultValue(false).setSaveConsumer(value -> {config.screenshot_hide_hud = value;}).build());
        // screenshot.addEntry(entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.screenshot_hide_hand"), config.screenshot_hide_hand).setTooltip(Translatable.get("config.discorddark.screenshot_hide_hand.tooltip")).setDefaultValue(false).setSaveConsumer(value -> {config.screenshot_hide_hand = value;}).build());
        
        ConfigCategory debug = builder.getOrCreateCategory(Translatable.get("category.discorddark.debug"));
        debug.addEntry(entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.show_debug_messages"), config.show_debug_messages).setTooltip(Translatable.get("config.discorddark.show_debug_messages.tooltip")).setDefaultValue(false).setSaveConsumer(value -> {config.show_debug_messages = value;}).build());
        debug.addEntry(entryBuilder.startIntSlider(Translatable.get("config.discorddark.screenshot_frameskip"), config.screenshot_frameskip, 1, 10).setTooltip(Translatable.get("config.discorddark.screenshot_frameskip.tooltip")).setDefaultValue(2).setSaveConsumer(value -> {config.screenshot_frameskip = value;}).build());

        ConfigCategory methods = builder.getOrCreateCategory(Translatable.get("category.discorddark.methods"));
        methods.addEntry(entryBuilder.startLongField(Translatable.get("config.discorddark.default_channel_id"), config.default_channel_id).setTooltip(Translatable.get("config.discorddark.default_channel_id.tooltip")).setDefaultValue(0L).setSaveConsumer(value -> {config.default_channel_id = value;}).build());
        methods.addEntry(entryBuilder.startLongField(Translatable.get("config.discorddark.backend_channel_id"), config.backend_channel_id).setTooltip(Translatable.get("config.discorddark.backend_channel_id.tooltip")).setDefaultValue(0L).setSaveConsumer(value -> {config.backend_channel_id = value;}).build());

        methods.addEntry(new NestedListListEntry<SendMethod, MultiElementListEntry<SendMethod>>(
            Translatable.get("config.discorddark.methods"), // fieldName
            config.send_methods, // value
            true, // defaultExpanded
            Optional::empty, // tooltipSupplier | TODO
            list -> {config.send_methods = list;}, // saveConsumer
            () -> { return getDefaultMethodList(); }, // defaultValue
            entryBuilder.getResetButtonKey(), // resetButtonKey
            true, // deleteButtonEnabled
            true, // insertInFront
            (_method, _nestedListListEntry) -> { // createNewCell
                if (_method == null) {
                    _method = new SendMethod();
                }
                final SendMethod method = _method;

                return new MultiElementListEntry<>(Translatable.get("config.discorddark.method_details"), method,
                    Lists.newArrayList(
                        entryBuilder.startStrField(Translatable.get("config.discorddark.method.identifier"), method.identifier).setDefaultValue(SendMethod.d_identifier).setTooltip(Translatable.get("config.discorddark.method.identifier.tooltip")).setSaveConsumer(value -> {method.identifier = value;}).build(),
                        entryBuilder.startLongField(Translatable.get("config.discorddark.method.guild_id"), method.guild_id).setDefaultValue(SendMethod.d_guild_id).setTooltip(Translatable.get("config.discorddark.method.guild_id.tooltip")).setSaveConsumer(value -> {method.guild_id = value;}).build(),
                        entryBuilder.startLongField(Translatable.get("config.discorddark.method.channel_id"), method.channel_id).setDefaultValue(SendMethod.d_channel_id).setTooltip(Translatable.get("config.discorddark.method.channel_id.tooltip")).setSaveConsumer(value -> {method.channel_id = value;}).build(),
                        entryBuilder.startColorField(Translatable.get("config.discorddark.method.embed_colour"), method.embed_colour).setDefaultValue(SendMethod.d_embed_colour).setTooltip(Translatable.get("config.discorddark.method.embed_colour.tooltip")).setSaveConsumer(value -> {method.embed_colour = value;}).build(),
                        entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.method.use_dimension_colour"), method.use_dimension_colour).setDefaultValue(SendMethod.d_use_dimension_colour).setTooltip(Translatable.get("config.discorddark.method.use_dimension_colour.tooltip")).setSaveConsumer(value -> {method.use_dimension_colour = value;}).build(),
                        entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.method.play_sound"), method.play_sound).setDefaultValue(SendMethod.d_play_sound).setTooltip(Translatable.get("config.discorddark.method.play_sound.tooltip")).setSaveConsumer(value -> {method.play_sound = value;}).build(),
                        entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.method.require_name"), method.require_name).setDefaultValue(SendMethod.d_require_name).setTooltip(Translatable.get("config.discorddark.method.require_name.tooltip")).setSaveConsumer(value -> {method.require_name = value;}).build(),
                        
                        entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.method.inline_fields"), method.inline_fields).setDefaultValue(SendMethod.d_inline_fields).setTooltip(Translatable.get("config.discorddark.method.inline_fields.tooltip")).setSaveConsumer(value -> {method.inline_fields = value;}).build(),
                        entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.method.include_name"), method.include_name).setDefaultValue(SendMethod.d_include_name).setTooltip(Translatable.get("config.discorddark.method.include_name.tooltip")).setSaveConsumer(value -> {method.include_name = value;}).build(),
                        entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.method.include_player"), method.include_player).setDefaultValue(SendMethod.d_include_player).setTooltip(Translatable.get("config.discorddark.method.include_player.tooltip")).setSaveConsumer(value -> {method.include_player = value;}).build(),
                        entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.method.include_screenshot"), method.include_screenshot).setDefaultValue(SendMethod.d_include_screenshot).setTooltip(Translatable.get("config.discorddark.method.include_screenshot.tooltip")).setSaveConsumer(value -> {method.include_screenshot = value;}).build(),
                        entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.method.hide_hud"), method.hide_hud).setDefaultValue(SendMethod.d_hide_hud).setTooltip(Translatable.get("config.discorddark.method.hide_hud.tooltip")).setSaveConsumer(value -> {method.hide_hud = value;}).build(),
                        entryBuilder.startBooleanToggle(Translatable.get("config.discorddark.method.hide_hand"), method.hide_hand).setDefaultValue(SendMethod.d_hide_hand).setTooltip(Translatable.get("config.discorddark.method.hide_hand.tooltip")).setSaveConsumer(value -> {method.hide_hand = value;}).build(),

                        new NestedListListEntry<SendMethod.InfoType, EnumListEntry<SendMethod.InfoType>>(
                            Translatable.get("config.discorddark.method.field_info"),
                            method.field_info,
                            false,
                            () -> Optional.ofNullable(SendMethod.getInfoTypeTooltip(Translatable.gets("config.discorddark.method.field_info.tooltip"))),
                            list -> {method.field_info = list;},
                            () -> Lists.newArrayList(),
                            entryBuilder.getResetButtonKey(),
                            true,
                            false,
                            (info_type, _nestedListListEntryB) -> {
                                if (info_type == null) {
                                    info_type = SendMethod.InfoType.NAME;
                                }
                                return entryBuilder.startEnumSelector(Translatable.get("config.discorddark.method.info_type"), SendMethod.InfoType.class, info_type).setDefaultValue(SendMethod.InfoType.NAME).build();
                            }
                        ),

                        new NestedListListEntry<SendMethod.InfoType, EnumListEntry<SendMethod.InfoType>>(
                            Translatable.get("config.discorddark.method.footer_info"),
                            method.footer_info,
                            false,
                            () -> Optional.ofNullable(SendMethod.getInfoTypeTooltip(Translatable.gets("config.discorddark.method.footer_info.tooltip"))),
                            list -> {method.footer_info = list;},
                            () -> Lists.newArrayList(),
                            entryBuilder.getResetButtonKey(),
                            true,
                            false,
                            (info_type, _nestedListListEntryB) -> {
                                if (info_type == null) {
                                    info_type = SendMethod.InfoType.NAME;
                                }
                                return entryBuilder.startEnumSelector(Translatable.get("config.discorddark.method.info_type"), SendMethod.InfoType.class, info_type).setDefaultValue(SendMethod.InfoType.NAME).build();
                            }
                        )
                        
                    ), true);
            }
        ));

        builder.setSavingRunnable(() -> {
            saveConfig();
        });

        return builder.build();
    }

    static private List<SendMethod> getDefaultMethodList() {
        SendMethod location = new SendMethod();
        location.identifier = "location";
        location.use_dimension_colour = true;
        location.include_screenshot = true;
        location.include_name = true;
        location.include_player = true;
        location.hide_hud = true;
        location.hide_hand = true;
        location.play_sound = true;
        location.require_name = true;
        location.field_info = Lists.newArrayList(
            SendMethod.InfoType.COORDS, SendMethod.InfoType.BIOME, SendMethod.InfoType.DIMENSION
        );

        SendMethod screenshot = new SendMethod();
        screenshot.identifier = "screenshot";
        screenshot.use_dimension_colour = false;
        screenshot.include_screenshot = true;
        screenshot.include_name = true;
        screenshot.include_player = true;
        screenshot.hide_hud = false;
        screenshot.hide_hand = false;
        screenshot.play_sound = true;
        screenshot.require_name = false;
        screenshot.footer_info = Lists.newArrayList(
            SendMethod.InfoType.COORDS
        );

        return Lists.newArrayList(location, screenshot);
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
