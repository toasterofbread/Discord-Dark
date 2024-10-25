package com.spectreseven1138.discorddark.mixins;

import com.spectreseven1138.discorddark.DiscordDark;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class Client {
    @Inject(at = @At("HEAD"), method = "render(Z)V")
    private void render(CallbackInfo ci) {
        if (DiscordDark.screenshot_request.shouldProvideScreenshot()) {
            DiscordDark.screenshot_request.provideScreenshot(ScreenshotRecorder.takeScreenshot(MinecraftClient.getInstance().getFramebuffer()));
        }
    }
}