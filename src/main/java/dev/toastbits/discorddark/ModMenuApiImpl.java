package dev.toastbits.discorddark;

import static net.fabricmc.api.EnvType.CLIENT;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(CLIENT)
public class ModMenuApiImpl implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
			return Config::buildMenu;
		} else {
			return ModMenuApi.super.getModConfigScreenFactory();
		}
	}
}