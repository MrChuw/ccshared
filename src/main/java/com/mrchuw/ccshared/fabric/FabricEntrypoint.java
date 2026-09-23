package com.mrchuw.ccshared.fabric;

//? if fabric {
import com.mrchuw.ccshared.CCShared;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FabricEntrypoint implements ModInitializer {

    @Override
    public void onInitialize() {
        CCShared.init();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            FabricPlatformImpl.setServer(server);

            Path worldSaveDir = server.getWorldPath(LevelResource.ROOT);
            Path sharedFolder = worldSaveDir.resolve("computercraft").resolve("shared");
            try {
                Files.createDirectories(sharedFolder);
                CCShared.LOGGER.info("[cc: Shared] Pasta compartilhada criada em: {}", sharedFolder);
            } catch (IOException e) {
                CCShared.LOGGER.error("[cc: Shared] Falha ao criar pasta do CC:", e);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> FabricPlatformImpl.setServer(null));
    }
}
//?}