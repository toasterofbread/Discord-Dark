package com.spectreseven1138.discorddark;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.EmbedBuilder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.entity.player.PlayerEntity;
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
import com.google.gson.Gson;

import com.spectreseven1138.discorddark.Config;

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

    enum EmbedType {
        MARK_LOCATION, SCREENSHOT
    }

    public void sendEmbed(EmbedType type, String name, NativeImage image, PlayerEntity player) {

        if (Config.get().guild_id == 0L) {
            logger.log("No guild ID set", 2, false);
            return;
        }
        Guild guild = bot.getGuildById(Config.get().guild_id);
        if (guild == null) {
            logger.log(String.format("Could not get guild with ID '%d'", Config.get().guild_id), 2, false);
            return;
        }

        if (Config.get().location_channel_id == 0L) {
            logger.log("No location channel ID set", 2, false);
            return;
        }

        long channel_id;
        switch (type) {
            case MARK_LOCATION: channel_id = Config.get().location_channel_id; break;
            case SCREENSHOT: channel_id = Config.get().screenshot_channel_id; break;
            default: channel_id = 0L;
        }

        MessageChannel channel = guild.getTextChannelById(channel_id);
        if (channel == null) {
            logger.log(String.format("Could not get channel with ID '%d'", channel_id), 2, false);
            return;
        }

        if (Config.get().backend_channel_id == 0L) {
            logger.log("No backend channel ID set", 2, false);
            return;
        }
        MessageChannel backend_channel = guild.getTextChannelById(Config.get().backend_channel_id);
        if (backend_channel == null) {
            logger.log(String.format("Could not get backend channel with ID '%d'", Config.get().backend_channel_id), 2, false);
            return;
        }

        byte[] bytes;

        try {
            bytes = image.getBytes();
        } catch (Exception e) {
            logger.log(e.getMessage(), 2, false);
            return;
        }

        double x = player.getX(), y = player.getY(), z = player.getZ();

        World world = player.world;
        String player_name = player.getDisplayName().getString();

        EmbedBuilder embed = new EmbedBuilder();

        if (name.length() > 0) {
            embed.setTitle(name, null);
        }

        if (type == EmbedType.MARK_LOCATION) {
            String dimension = world.getRegistryKey().getValue().toString();
            switch (dimension) {
                case "minecraft:overworld": embed.setColor(Color.green); dimension = "Overworld"; break;
                case "minecraft:the_nether": embed.setColor(Color.red); dimension = "Nether"; break;
                case "minecraft:the_end": embed.setColor(Color.magenta); dimension = "The End"; break;
                default: embed.setColor(Color.gray); break;
            }

            String biome = world.getBiomeAccess().getBiome(new BlockPos(x, y, z)).getKey().get().getValue().toString();

            if (biome.startsWith("minecraft:")) {
                biome = biome.substring(10).replace("_", " ");
                biome = biome.substring(0, 1).toUpperCase() + biome.substring(1);
            }

            embed.addField("Coordinates", String.format("%d, %d, %d", (int)Math.round(x), (int)Math.round(y), (int)Math.round(z)), true);
            embed.addField("Biome", biome, true);
            embed.addField("Dimension", dimension, true);
        }
        else if (type == EmbedType.SCREENSHOT) {
            embed.setFooter(String.format("%d, %d, %d", (int)Math.round(x), (int)Math.round(y), (int)Math.round(z)));
        }

        embed.setAuthor(player_name, null, String.format("https://mc-heads.net/avatar/%s.png", player_name));

        if (Config.get().play_sounds) {
            world.playSound(
                x, y, z,
                new SoundEvent(new Identifier("minecraft:block.sculk_sensor.clicking"), 15f),
                SoundCategory.BLOCKS,
                1f,
                1f,
                true
            );
        }
        
        backend_channel.sendFile(bytes, "screenshot.png").queue(response -> {
            embed.setImage(response.getAttachments().get(0).getUrl());
            channel.sendMessageEmbeds(embed.build()).queue();

            String message;
            switch (type) {
                case MARK_LOCATION: message = "Location marked in [%s | #%s]"; break;
                case SCREENSHOT: message = "Screenshot uploaded to [%s | #%s]"; break;
                default: message = "";
            }

            logger.log(String.format(message, guild.getName(), channel.getName()), 0, false);
        });
    }

    private void send(MessageReceivedEvent event, String message) {
        event.getChannel().sendMessage(message).queue();
    }

    private void send(MessageReceivedEvent event, String message, int log_level) {
        send(event, message);
        logger.log(message, log_level, false);
    }
}
