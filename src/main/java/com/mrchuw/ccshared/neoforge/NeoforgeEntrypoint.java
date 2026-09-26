package com.mrchuw.ccshared.neoforge;

//? if neoforge {
/*import com.mrchuw.ccshared.CCShared;
import com.mrchuw.ccshared.data.RecipeIndex;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(CCShared.MOD_ID)
public class NeoforgeEntrypoint {

    public NeoforgeEntrypoint(IEventBus modEventBus) {
        CCShared.init();
        NeoForge.EVENT_BUS.addListener(this::onServerAboutToStart);
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        var server = event.getServer();

        Path worldSaveDir = server.getWorldPath(LevelResource.ROOT);
        Path sharedFolder = worldSaveDir.resolve("computercraft").resolve("shared");
        try {
            Files.createDirectories(sharedFolder);
            CCShared.LOGGER.info("[cc: Shared] Shared folder created at: {}", sharedFolder);
        } catch (IOException e) {
            CCShared.LOGGER.error("[cc: Shared] Failed to create CC folder:", e);
        }
        RecipeIndex.rebuild(server);
    }
}
*///?}