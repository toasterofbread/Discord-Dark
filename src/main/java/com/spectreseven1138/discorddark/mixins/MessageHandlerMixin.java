package com.spectreseven1138.discorddark.mixins;

import com.mojang.authlib.GameProfile;
import com.spectreseven1138.discorddark.Config;
import com.spectreseven1138.discorddark.DiscordDark;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(MessageHandler.class)
public abstract class MessageHandlerMixin {
	@Inject(at = @At("HEAD"), method = "onChatMessage", cancellable = true)
	public void onChatMessage(SignedMessage message, GameProfile sender, MessageType.Parameters params, CallbackInfo ci) {
		System.out.println("onChatMessage called");

		if (sender == null) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null) {
			return;
		}



		String content = message.getContent().getString();
		if (content == null || content.isEmpty()) {
			return;
		}

		if (!content.startsWith(String.format("[%s] Sent ", DiscordDark.MOD_NAME))) {
			return;
		}

		if (sender.getId() != player.getUuid()) {

			if (Config.get().play_external_sounds) {
				World world = player.getWorld();
				for (AbstractClientPlayerEntity other_player : player.clientWorld.getPlayers()) {
					if (other_player.getUuid() != sender.getId()) {
						continue;
					}

					if (other_player.getWorld().getRegistryKey() != world.getRegistryKey()) {
						continue;
					}

					world.playSound(
						other_player.getX(), other_player.getY(), other_player.getZ(),
						new SoundEvent(Identifier.of("minecraft:block.sculk_sensor.clicking"), Optional.of(15f)),
						SoundCategory.BLOCKS,
						1f,
						1f,
						true
					);
					break;
				}
			}

			content = String.format("[%s] %s sent %s", DiscordDark.MOD_NAME, sender.getName(), content.substring(DiscordDark.MOD_NAME.length() + 8));
		}

		client.inGameHud.getChatHud().addMessage(Text.literal(content));
		ci.cancel();
	}
}