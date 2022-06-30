package com.spectreseven1138.discorddark.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHudListener;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.MessageSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jetbrains.annotations.Nullable;
import net.minecraft.text.Text;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import com.spectreseven1138.discorddark.DiscordDark;
import com.spectreseven1138.discorddark.Config;

@Mixin(ChatHudListener.class)
public abstract class ChatHudListenerMixin {

	@Inject(at = @At("HEAD"), method = "onChatMessage", cancellable = true)
	public void onChatMessageReceived(MessageType messageType, Text text, @Nullable MessageSender sender, CallbackInfo ci) {
		if (sender == null) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null) {
			return;
		}
		
		String message = text.getString();
		if (message == null || message.isEmpty()) {
			return;
		}

		if (!message.startsWith(String.format("[%s] Sent ", DiscordDark.MOD_NAME))) {
			return;
		}

		if (sender.uuid() != player.getUuid()) {
			
			if (Config.get().play_external_sounds) {
				World world = player.world;
				for (AbstractClientPlayerEntity other_player : player.clientWorld.getPlayers()) {
					if (other_player.getUuid() != sender.uuid()) {
						continue;
					}

					if (other_player.world.getRegistryKey() != world.getRegistryKey()) {
						continue;
					}

					world.playSound(
						other_player.getX(), other_player.getY(), other_player.getZ(),
						new SoundEvent(new Identifier("minecraft:block.sculk_sensor.clicking"), 15f),
						SoundCategory.BLOCKS,
						1f,
						1f,
						true
					);
					break;
				}
			}

			message = String.format("[%s] %s sent %s", DiscordDark.MOD_NAME, sender.name().getString(), message.substring(DiscordDark.MOD_NAME.length() + 8));
		}

		client.inGameHud.getChatHud().addMessage(Text.literal(message));
		ci.cancel();
	}
}