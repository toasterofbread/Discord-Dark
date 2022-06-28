package com.spectreseven1138.discorddark.mixins;

import com.spectreseven1138.discorddark.DiscordDark;

import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class HUD {
    @Inject(at=@At("HEAD"), method = "render", cancellable = true)
    public void render(CallbackInfo info) {
        if (DiscordDark.screenshot_request.shouldHideHud()) {
            info.cancel();
        }
    }
}
