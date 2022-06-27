package com.spectreseven1138.discordintegration.mixins;

import com.spectreseven1138.discordintegration.DiscordIntegration;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class Client {
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BackgroundRenderer;method_23792()V", ordinal = 0), method = "render(Z)V")
    private void onRender(CallbackInfo ci) {
        switch (DiscordIntegration.awaiting_screenshot) {
            case 1: DiscordIntegration.awaiting_screenshot = 2; break;
            case 2: DiscordIntegration.getInstance().provideScreenshot(ScreenshotRecorder.takeScreenshot(MinecraftClient.getInstance().getFramebuffer())); break;
        }
    }
}