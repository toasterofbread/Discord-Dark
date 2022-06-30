package com.spectreseven1138.discorddark;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageHistory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.util.Session;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import javax.security.auth.login.LoginException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Consumer;
import com.google.gson.Gson;

import com.spectreseven1138.discorddark.Config;
import com.spectreseven1138.discorddark.Utils.Translatable;
import com.spectreseven1138.discorddark.SendMethod;

public class Bot extends ListenerAdapter {

    private static final String owner_id = "402344993391640578";

    private JDA bot;
    private String prefix;
    private MessageChannel owner_channel;
    private MinecraftClient client;

    public boolean case_sensitive = false;

    public interface LogCallback {
        public void log(String message, int level, boolean debug);
    }
    private LogCallback logger;

    Bot(String token, String prefix, LogCallback logger) throws LoginException {
        bot = JDABuilder.createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
            .addEventListeners(this)
            .build();

        bot.getPresence().setActivity(Activity.playing("Minecraft"));

        this.prefix = prefix;
        this.logger = logger;

        owner_channel = bot.retrieveApplicationInfo().complete().getOwner().openPrivateChannel().complete();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface Command {
        String trigger();
        AccessType accesstype();
        int argcount() default 0; // If negative, excess arguments will be added to the last item

        enum AccessType {
            CLIENT, // The command can be invoked normally by users
            SERVER,  // The command can only by the bot itself (for use by the server instance)
            ANY
        }
    }

    @Command(trigger="notifyplayer", argcount=-3, accesstype=Command.AccessType.ANY)
    public void commandNotifyPlayer(List<String> args, MessageReceivedEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        if (!args.get(0).equals(client.player.getDisplayName().getString())) {
            return;
        }

        int level;
        switch (args.get(1)) {
            case "0": level = 0; break;
            case "1": level = 1; break;
            case "2": level = 2; break;
            default:  logger.log(String.format("Unknown log level '%s'", args.get(1)), 1, true); level = 0; break;
        }

        logger.log(args.get(2), level, false);
    }

    @Command(trigger="setprefix", argcount=-1, accesstype=Command.AccessType.CLIENT)
    public void commandSetPrefix(List<String> args, MessageReceivedEvent event) {
        Gson g = new Gson();
        
        try {
            String new_prefix = g.fromJson(args.get(0), String.class);
            prefix = new_prefix;
        }
        catch (Exception e) {
            send(event, String.format("Caught an exception while parsing prefix: %s", e.getMessage()), 2);
            return;
        }
    }

    @Command(trigger="listclients", argcount=0, accesstype=Command.AccessType.CLIENT)
    public void commandListClients(List<String> args, MessageReceivedEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        Session session = client.getSession();
        send(event, String.format(" - %s [%s] In world: %b", session.getUsername(), session.getUuid(), client.player));
    }

    private MessageChannel getChannelFromMessage(String message, Guild guild) {
        if (!message.startsWith("<#")) {
            return null;
        }
        if (!message.endsWith(">")) {
            return null;
        }

        return guild.getTextChannelById(message.substring(2, message.length() - 1));
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        boolean is_self = event.getAuthor().getIdLong() == bot.getSelfUser().getIdLong();

        if (event.getAuthor().isBot() && !is_self) {
            return;
        }

        String command = event.getMessage().getContentRaw();

        if (command.length() < prefix.length()) {
            return;
        }

        String command_prefix = command.substring(0, prefix.length());
        if (!case_sensitive) {
            command_prefix = command_prefix.toLowerCase();
        }

        if (!command_prefix.equals(prefix)) {
            return;
        }

        command = command.substring(prefix.length());
        int split = command.indexOf(" ");

        String arguments;

        if (split != -1) {
            arguments = command.substring(split + 1);
            command = command.substring(0, split);
        }
        else {
            arguments = "";
        }

        if (!case_sensitive) {
            command = command.toLowerCase();
        }

        for (Method method : getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Command.class)) {
                Command command_info = (Command) method.getAnnotation(Command.class);

                if (command_info.accesstype() != Command.AccessType.ANY && ((command_info.accesstype() == Command.AccessType.CLIENT) == is_self)) {
                    continue;
                }

                if (command.equals(command_info.trigger())) {
                    ArgParser parser = new ArgParser(command_info.argcount());
                    List<String> parsed = new ArrayList<String>();
                    String err = parser.parseArguments(arguments, parsed);

                    if (err.length() > 0) {
                        send(event, "Failed to parse arguments: " + err, 2);
                        return;
                    }

                    logger.log("Invoking bot command '" + command + "'", 0, true);
                    
                    try {
                        method.invoke(this, parsed, event);
                    } catch (Exception e) {
                        send(event, "Caught exception while invoking command " + command + ": " + e.getMessage(), 2);
                    }

                    return;
                }
            }
        }
    }

    public void sendEmbed(SendMethod method, String name, NativeImage image, ClientPlayerEntity player) {

        long guild_id = method.guild_id == 0L ? Config.get().guild_id : method.guild_id;
        if (guild_id == 0L) {
            logger.log("No guild ID set", 2, false);
            return;
        }
        Guild guild = bot.getGuildById(guild_id);
        if (guild == null) {
            logger.log(String.format("Could not get guild with ID '%d'", guild_id), 2, false);
            return;
        }

        long channel_id = method.channel_id == 0L ? Config.get().default_channel_id : method.channel_id;
        if (channel_id == 0L) {
            logger.log("No channel ID set", 2, false);
            return;
        }
        MessageChannel channel = guild.getTextChannelById(channel_id);
        if (channel == null) {
            logger.log(String.format("Could not get channel with ID '%d'", channel_id), 2, false);
            return;
        }

        MessageChannel backend_channel = null;
        if (method.include_screenshot) {
            if (Config.get().backend_channel_id == 0L) {
                logger.log("No backend channel ID set", 2, false);
                return;
            }
            backend_channel = guild.getTextChannelById(Config.get().backend_channel_id);
            if (backend_channel == null) {
                logger.log(String.format("Could not get backend channel with ID '%d'", Config.get().backend_channel_id), 2, false);
                return;
            }
        }

        double x = player.getX(), y = player.getY(), z = player.getZ();

        World world = player.world;

        String player_name = player.getDisplayName().getString();
        String biome = world.getBiomeAccess().getBiome(new BlockPos(x, y, z)).getKey().get().getValue().toString();
        String biome_formatted;
        if (biome.startsWith("minecraft:")) {
            biome_formatted = biome.substring(10).replace("_", " ");
            biome_formatted = biome_formatted.substring(0, 1).toUpperCase() + biome_formatted.substring(1);
        }
        else {
            biome_formatted = biome;
        }

        String dimension = world.getRegistryKey().getValue().toString();
        final String dimension_formatted;
        switch (dimension) {
            case "minecraft:overworld": dimension_formatted = Translatable.gets("dimension.discorddark.overworld"); break;
            case "minecraft:the_nether": dimension_formatted = Translatable.gets("dimension.discorddark.nether"); break;
            case "minecraft:the_end": dimension_formatted = Translatable.gets("dimension.discorddark.end"); break;
            default: dimension_formatted = dimension; break;
        }

        EmbedBuilder embed = new EmbedBuilder();

        if (method.include_name && name.length() > 0) {
            embed.setTitle(name, null);
        }

        if (method.include_player) {
            embed.setAuthor(player_name, null, String.format("https://mc-heads.net/avatar/%s.png", player_name));
        }

        if (method.use_dimension_colour) {
            switch (dimension) {
                case "minecraft:overworld": embed.setColor(Color.GREEN); break;
                case "minecraft:the_nether": embed.setColor(Color.RED); break;
                case "minecraft:the_end": embed.setColor(Color.MAGENTA); break;
            }
        }
        else {
            embed.setColor(method.embed_colour);
        }

        Function<SendMethod.InfoType, String> getInfoName = (info_type) -> {
            switch (info_type) {
                case NAME: return Translatable.gets("infotype.discorddark.name");
                case PLAYER: return Translatable.gets("infotype.discorddark.player");
                case COORDS: return Translatable.gets("infotype.discorddark.coords");
                case BIOME: return Translatable.gets("infotype.discorddark.biome");
                case DIMENSION: return Translatable.gets("infotype.discorddark.dimension");
                default: return "";
            }
        };

        final String _biome_formatted = biome_formatted;
        Function<SendMethod.InfoType, String> getInfoValue = (info_type) -> {
            switch (info_type) {
                case NAME: return name;
                case PLAYER: return player_name;
                case COORDS: {
                    String prefix;
                    switch (dimension) {
                        case "minecraft:overworld": prefix = "\u200b"; break;
                        case "minecraft:the_nether": prefix = "\u200b\u200b"; break;
                        case "minecraft:the_end": prefix = "\u200b\u200b\u200b"; break;
                        default: prefix = ""; break;
                    }
                    return prefix + String.format("%.1f, %.1f, %.1f", x, y, z);
                }
                case BIOME: return _biome_formatted;
                case DIMENSION: return dimension_formatted;
                default: return "";
            }
        };

        for (SendMethod.InfoType info_type : method.field_info) {
            String key = getInfoName.apply(info_type);
            String value = getInfoValue.apply(info_type);
            if (key.length() > 0 && value.length() > 0) {
                embed.addField(key, value, method.inline_fields);
            }
        }

        if (!method.footer_info.isEmpty()) {
            String footer = "";
            boolean first = true;
            for (SendMethod.InfoType info_type : method.footer_info) {
                String value = getInfoValue.apply(info_type);
                if (value.length() == 0) {
                    continue;
                }

                if (first) {
                    first = false;
                }
                else {
                    footer += " | ";
                }

                footer += value;
            }
            embed.setFooter(footer);
        }

        if (method.play_sound) {
            world.playSound(
                x, y, z,
                new SoundEvent(new Identifier("minecraft:block.sculk_sensor.clicking"), 15f),
                SoundCategory.BLOCKS,
                1f,
                1f,
                true
            );
        }
        
        Consumer<EmbedBuilder> send = (e) -> {
            channel.sendMessageEmbeds(e.build()).queue();

            String message = String.format("Sent %s %sto [%s | #%s]", method.identifier, name.length() == 0 ? "" : String.format("'%s' ", name), guild.getName(), channel.getName());
            if (method.notify) {
                player.sendChatMessage(String.format("[%s] %s", DiscordDark.MOD_NAME, message));
            }
            else {
                logger.log(message, 0, false);
            }

        };

        if (method.include_screenshot && image != null) {
            byte[] bytes;
            try {
                bytes = image.getBytes();
            } catch (Exception e) {
                logger.log(e.getMessage(), 2, false);
                return;
            }

            backend_channel.sendFile(bytes, "screenshot.png").queue(response -> {
                embed.setImage(response.getAttachments().get(0).getUrl());
                send.accept(embed);
            });
        }
        else {
            send.accept(embed);
        }
    }

    public class EmbedInfo {
        String name;
        boolean has_name = false;

        String biome;
        boolean has_biome = false;

        String dimension;
        boolean has_dimension = false;

        String player;
        boolean has_player = false;
        
        String image_url;
        boolean has_image = false;

        double x;
        double y;
        double z;
        boolean has_coords = false;

        int i;
    }

    public String iterateMethodEmbeds(Function<EmbedInfo, Boolean> callback, SendMethod method) {
        long guild_id = method.guild_id == 0L ? Config.get().guild_id : method.guild_id;
        if (guild_id == 0L) {
            return "No guild ID set";
        }
        Guild guild = bot.getGuildById(guild_id);
        if (guild == null) {
            return String.format("Could not get guild with ID '%d'", guild_id);
        }

        long channel_id = method.channel_id == 0L ? Config.get().default_channel_id : method.channel_id;
        if (channel_id == 0L) {
            return "No channel ID set";
        }
        MessageChannel channel = guild.getTextChannelById(channel_id);
        if (channel == null) {
            return String.format("Could not get channel with ID '%d'", channel_id);
        }
        
        User self = bot.getSelfUser();

        channel.getIterableHistory().forEachAsync(message -> {
            if (!message.getAuthor().equals(self)) {
                return true;
            }

            List<MessageEmbed> embeds = message.getEmbeds();
            if (embeds.isEmpty()) {
                return true;
            }

            MessageEmbed embed = embeds.get(0);
            
            EmbedInfo info = new EmbedInfo();
            
            if (embed.getTitle() != null) {
                info.name = embed.getTitle();
                info.has_name = true;
            }

            if (embed.getAuthor() != null) {
                info.player = embed.getAuthor().getName();
                info.has_player = true;
            }

            if (embed.getImage() != null) {
                info.image_url = embed.getImage().getUrl();
                info.has_image = true;
            }

            for (MessageEmbed.Field field : embed.getFields()) {
                final String name = field.getName();
                final String value = field.getValue();

                if (name.equals(Translatable.gets("infotype.discorddark.name"))) {
                    info.name = value;
                    info.has_name = true;
                }
                else if (name.equals(Translatable.gets("infotype.discorddark.player"))) {
                    info.player = value;
                    info.has_player = true;
                }
                else if (name.equals(Translatable.gets("infotype.discorddark.coords"))) {
                    String current = "";

                    int coord = 0;
                    int dimension = -1;

                    for (int i = 0, length = value.length(); i < length; i++) {
                        final char c = value.charAt(i);
                        if (c == ' ') {
                            continue;
                        }
                        else if (c == ',') {
                            if (coord++ == 0) {
                                info.x = Double.parseDouble(current);
                            }
                            else {
                                info.y = Double.parseDouble(current);
                            }
                            current = "";
                        }
                        else if (c == '\u200b') {
                            dimension++;
                        }
                        else {
                            current += c;
                        }
                    }

                    info.z = Double.parseDouble(current);
                    info.has_coords = true;

                    if (dimension > -1) {
                        switch (dimension) {
                            case 0: info.dimension = "minecraft:overworld"; break;
                            case 1: info.dimension = "minecraft:the_nether"; break;
                            case 2: info.dimension = "minecraft:the_end"; break;
                        }
                        info.has_dimension = true;
                    }
                }
                else if (name.equals(Translatable.gets("infotype.discorddark.biome"))) {
                    info.biome = value;
                    info.has_biome = true;
                }
                else if (name.equals(Translatable.gets("infotype.discorddark.dimension"))) {
                    if (value.equals(Translatable.gets("dimension.discorddark.overworld"))) {
                        info.dimension = "minecraft:overworld";
                    }
                    else if (value.equals(Translatable.gets("dimension.discorddark.nether"))) {
                        info.dimension = "minecraft:the_nether";
                    }
                    else if (value.equals(Translatable.gets("dimension.discorddark.end"))) {
                        info.dimension = "minecraft:the_end";
                    }
                    else {
                        info.dimension = value;
                    }
                    info.has_dimension = true;
                }
                else {
                    logger.log(name, 2, true);
                }
            }

            info.i++;
            return callback.apply(info);
        });

        return "";
    }

    private void send(MessageReceivedEvent event, String message) {
        event.getChannel().sendMessage(message).queue();
    }

    private void send(MessageReceivedEvent event, String message, int log_level) {
        send(event, message);
        logger.log(message, log_level, false);
    }
}
