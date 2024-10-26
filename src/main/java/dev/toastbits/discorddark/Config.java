package dev.toastbits.discorddark;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Clipboard;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.screen.Screen;

import net.fabricmc.loader.api.FabricLoader;

import dev.toastbits.discorddark.component.ButtonListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
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

import javax.annotation.Nullable;

public class Config {

    public static class ConfigFormat {
        public String bot_token = "";
        public boolean play_external_sounds = true;

        public long guild_id = 0L;
        public long default_channel_id = 0L;
        public long backend_channel_id = 0L;
        
        public boolean show_debug_messages = false;
        public int screenshot_frameskip = 2;

        public List<SendMethod> send_methods = getDefaultMethodList();

        ConfigFormat() {}
    }

    public static Screen buildMenu(Screen parent) {
        loadConfig();

        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Utils.Translatable.get("title.discorddark.config"));
        builder.setDefaultBackgroundTexture(Identifier.of("minecraft:textures/block/sculk_catalyst_top.png"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Utils.Translatable.get("category.discorddark.general"));
        general.addEntry(entryBuilder.startStrField(Utils.Translatable.get("config.discorddark.bot_token"), config.bot_token).setTooltip(Utils.Translatable.get("config.discorddark.bot_token.tooltip")).setDefaultValue("").setSaveConsumer(value -> {config.bot_token = value;}).build());
        general.addEntry(entryBuilder.startLongField(Utils.Translatable.get("config.discorddark.default_guild_id"), config.guild_id).setTooltip(Utils.Translatable.get("config.discorddark.default_guild_id.tooltip")).setDefaultValue(0L).setSaveConsumer(value -> {config.guild_id = value;}).build());

        general.addEntry(new ButtonListEntry(Utils.Translatable.get("config.discorddark.copy_config_to_clipboard"), Config::copyConfigToClipboard, false));
        general.addEntry(new ButtonListEntry(Utils.Translatable.get("config.discorddark.paste_config_from_clipboard"), Config::pasteConfigFromClipboard, false));

        ConfigCategory debug = builder.getOrCreateCategory(Utils.Translatable.get("category.discorddark.debug"));
        debug.addEntry(entryBuilder.startBooleanToggle(Utils.Translatable.get("config.discorddark.show_debug_messages"), config.show_debug_messages).setTooltip(Utils.Translatable.get("config.discorddark.show_debug_messages.tooltip")).setDefaultValue(false).setSaveConsumer(value -> {config.show_debug_messages = value;}).build());
        debug.addEntry(entryBuilder.startIntSlider(Utils.Translatable.get("config.discorddark.screenshot_frameskip"), config.screenshot_frameskip, 1, 10).setTooltip(Utils.Translatable.get("config.discorddark.screenshot_frameskip.tooltip")).setDefaultValue(2).setSaveConsumer(value -> {config.screenshot_frameskip = value;}).build());

        ConfigCategory methods = builder.getOrCreateCategory(Utils.Translatable.get("category.discorddark.methods"));
        methods.addEntry(entryBuilder.startLongField(Utils.Translatable.get("config.discorddark.default_channel_id"), config.default_channel_id).setTooltip(Utils.Translatable.get("config.discorddark.default_channel_id.tooltip")).setDefaultValue(0L).setSaveConsumer(value -> {config.default_channel_id = value;}).build());
        methods.addEntry(entryBuilder.startLongField(Utils.Translatable.get("config.discorddark.backend_channel_id"), config.backend_channel_id).setTooltip(Utils.Translatable.get("config.discorddark.backend_channel_id.tooltip")).setDefaultValue(0L).setSaveConsumer(value -> {config.backend_channel_id = value;}).build());

        methods.addEntry(new NestedListListEntry<SendMethod, MultiElementListEntry<SendMethod>>(
            Utils.Translatable.get("config.discorddark.methods"),
            config.send_methods,
            true,
            Optional::empty,
            list -> {config.send_methods = list;},
            () -> { return getDefaultMethodList(); },
            entryBuilder.getResetButtonKey(),
            true,
            true,
            (_method, _nestedListListEntry) -> {
                if (_method == null) {
                    _method = new SendMethod();
                }
                final SendMethod method = _method;

                return new MultiElementListEntry<>(Utils.Translatable.get("config.discorddark.method_details"), method,
                    Lists.newArrayList(
                        entryBuilder.startStrField(Utils.Translatable.get("config.discorddark.method.identifier"), method.identifier).setDefaultValue(SendMethod.d_identifier).setTooltip(Utils.Translatable.get("config.discorddark.method.identifier.tooltip")).setSaveConsumer(value -> {method.identifier = value;}).build(),
                        entryBuilder.startLongField(Utils.Translatable.get("config.discorddark.method.guild_id"), method.guild_id).setDefaultValue(SendMethod.d_guild_id).setTooltip(Utils.Translatable.get("config.discorddark.method.guild_id.tooltip")).setSaveConsumer(value -> {method.guild_id = value;}).build(),
                        entryBuilder.startLongField(Utils.Translatable.get("config.discorddark.method.channel_id"), method.channel_id).setDefaultValue(SendMethod.d_channel_id).setTooltip(Utils.Translatable.get("config.discorddark.method.channel_id.tooltip")).setSaveConsumer(value -> {method.channel_id = value;}).build(),
                        entryBuilder.startColorField(Utils.Translatable.get("config.discorddark.method.embed_colour"), method.embed_colour).setDefaultValue(SendMethod.d_embed_colour).setTooltip(Utils.Translatable.get("config.discorddark.method.embed_colour.tooltip")).setSaveConsumer(value -> {method.embed_colour = value;}).build(),
                        entryBuilder.startBooleanToggle(Utils.Translatable.get("config.discorddark.method.use_dimension_colour"), method.use_dimension_colour).setDefaultValue(SendMethod.d_use_dimension_colour).setTooltip(Utils.Translatable.get("config.discorddark.method.use_dimension_colour.tooltip")).setSaveConsumer(value -> {method.use_dimension_colour = value;}).build(),
                        entryBuilder.startBooleanToggle(Utils.Translatable.get("config.discorddark.method.play_sound"), method.play_sound).setDefaultValue(SendMethod.d_play_sound).setTooltip(Utils.Translatable.get("config.discorddark.method.play_sound.tooltip")).setSaveConsumer(value -> {method.play_sound = value;}).build(),
                        entryBuilder.startBooleanToggle(Utils.Translatable.get("config.discorddark.method.require_name"), method.require_name).setDefaultValue(SendMethod.d_require_name).setTooltip(Utils.Translatable.get("config.discorddark.method.require_name.tooltip")).setSaveConsumer(value -> {method.require_name = value;}).build(),
                        entryBuilder.startBooleanToggle(Utils.Translatable.get("config.discorddark.method.notify"), method.notify).setDefaultValue(SendMethod.d_notify).setTooltip(Utils.Translatable.get("config.discorddark.method.notify.tooltip")).setSaveConsumer(value -> {method.notify = value;}).build(),
                        
                        entryBuilder.startBooleanToggle(Utils.Translatable.get("config.discorddark.method.inline_fields"), method.inline_fields).setDefaultValue(SendMethod.d_inline_fields).setTooltip(Utils.Translatable.get("config.discorddark.method.inline_fields.tooltip")).setSaveConsumer(value -> {method.inline_fields = value;}).build(),
                        entryBuilder.startBooleanToggle(Utils.Translatable.get("config.discorddark.method.include_name"), method.include_name).setDefaultValue(SendMethod.d_include_name).setTooltip(Utils.Translatable.get("config.discorddark.method.include_name.tooltip")).setSaveConsumer(value -> {method.include_name = value;}).build(),
                        entryBuilder.startBooleanToggle(Utils.Translatable.get("config.discorddark.method.include_player"), method.include_player).setDefaultValue(SendMethod.d_include_player).setTooltip(Utils.Translatable.get("config.discorddark.method.include_player.tooltip")).setSaveConsumer(value -> {method.include_player = value;}).build(),
                        entryBuilder.startBooleanToggle(Utils.Translatable.get("config.discorddark.method.include_screenshot"), method.include_screenshot).setDefaultValue(SendMethod.d_include_screenshot).setTooltip(Utils.Translatable.get("config.discorddark.method.include_screenshot.tooltip")).setSaveConsumer(value -> {method.include_screenshot = value;}).build(),
                        entryBuilder.startBooleanToggle(Utils.Translatable.get("config.discorddark.method.hide_hud"), method.hide_hud).setDefaultValue(SendMethod.d_hide_hud).setTooltip(Utils.Translatable.get("config.discorddark.method.hide_hud.tooltip")).setSaveConsumer(value -> {method.hide_hud = value;}).build(),
                        entryBuilder.startBooleanToggle(Utils.Translatable.get("config.discorddark.method.hide_hand"), method.hide_hand).setDefaultValue(SendMethod.d_hide_hand).setTooltip(Utils.Translatable.get("config.discorddark.method.hide_hand.tooltip")).setSaveConsumer(value -> {method.hide_hand = value;}).build(),

                        new NestedListListEntry<SendMethod.InfoType, EnumListEntry<SendMethod.InfoType>>(
                            Utils.Translatable.get("config.discorddark.method.field_info"),
                            method.field_info,
                            false,
                            () -> Optional.ofNullable(SendMethod.getInfoTypeTooltip(Utils.Translatable.gets("config.discorddark.method.field_info.tooltip"))),
                            list -> {method.field_info = list;},
                            () -> Lists.newArrayList(),
                            entryBuilder.getResetButtonKey(),
                            true,
                            false,
                            (info_type, _nestedListListEntryB) -> {
                                if (info_type == null) {
                                    info_type = SendMethod.InfoType.NAME;
                                }
                                return entryBuilder.startEnumSelector(Utils.Translatable.get("config.discorddark.method.info_type"), SendMethod.InfoType.class, info_type).setDefaultValue(SendMethod.InfoType.NAME).build();
                            }
                        ),

                        new NestedListListEntry<SendMethod.InfoType, EnumListEntry<SendMethod.InfoType>>(
                            Utils.Translatable.get("config.discorddark.method.footer_info"),
                            method.footer_info,
                            false,
                            () -> Optional.ofNullable(SendMethod.getInfoTypeTooltip(Utils.Translatable.gets("config.discorddark.method.footer_info.tooltip"))),
                            list -> {method.footer_info = list;},
                            () -> Lists.newArrayList(),
                            entryBuilder.getResetButtonKey(),
                            true,
                            false,
                            (info_type, _nestedListListEntryB) -> {
                                if (info_type == null) {
                                    info_type = SendMethod.InfoType.NAME;
                                }
                                return entryBuilder.startEnumSelector(Utils.Translatable.get("config.discorddark.method.info_type"), SendMethod.InfoType.class, info_type).setDefaultValue(SendMethod.InfoType.NAME).build();
                            }
                        )
                        
                    ), true);
            }
        ));

        builder.setSavingRunnable(() -> {
            saveConfig();
        });

        screen = builder.build();
        return screen;
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
    private static Screen screen = null;
    public static MinecraftClient client = null;

    public static ConfigFormat get() {
        if (config == null) {
            loadConfig();
        }
        System.out.println("GET");
        System.out.println(config.bot_token);
        return config;
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("discorddark.json");
    }

    public static void loadConfig() {
        System.out.println("LOAD");
        Path path = getConfigPath();
        if (Files.exists(path)) {
            try (InputStream stream = Files.newInputStream(path)) {
                setConfigJson(IOUtils.toString(stream, StandardCharsets.UTF_8));
            }
            catch (IOException e) {
                throw new JsonParseException(e);
            }
        }
        else {
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
            Files.write(path, getConfigJson().getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e) {
            throw new JsonParseException(e);
        }
    }

    @Nullable
    private static Window getWindow() {
        if (client != null) {
            return client.getWindow();
        }

        try {
            MinecraftClient client = (MinecraftClient)Screen.class.getDeclaredField("client").get(screen);
            return client.getWindow();
        }
        catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void copyConfigToClipboard() {
        Window window = getWindow();
        if (window == null) {
            return;
        }

        Clipboard clipboard = new Clipboard();
        clipboard.setClipboard(window.getHandle(), getConfigJson());
    }

    private static void pasteConfigFromClipboard() {
        Window window = getWindow();
        if (window == null) {
            return;
        }

        Clipboard clipboard = new Clipboard();
        String json = clipboard.getClipboard(window.getHandle(), (int a, long b) -> {});

        try {
            setConfigJson(json);
        }
        catch (Throwable e) {
            System.out.println("JSON parsing failed");
            System.out.println(json);
            e.printStackTrace();
            return;
        }

        saveConfig();
        screen.close();
    }

    private static String getConfigJson() {
        Gson g = new Gson();
        return g.toJson(config);
    }

    private static void setConfigJson(String json) {
        Gson g = new Gson();
        config = g.fromJson(json, ConfigFormat.class);
    }
}
