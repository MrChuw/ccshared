package com.mrchuw.ccshared.mixin;

import com.mrchuw.ccshared.CCSharedPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.core.filesystem.FileSystem;
import net.minecraft.server.MinecraftServer;

@Mixin(targets = "dan200.computercraft.core.computer.ComputerExecutor", remap = false)
public abstract class ComputerExecutorMixin {

    @Unique
    private static final Logger sharedmount$LOGGER = LoggerFactory.getLogger("SharedMount");

    @Inject(method = "createFileSystem", at = @At("RETURN"))
    private void sharedmount$onCreateFileSystem(CallbackInfoReturnable<FileSystem> cir) {
        FileSystem filesystem = cir.getReturnValue();
        if (filesystem == null) {
            sharedmount$LOGGER.debug("[cc: Shared] Filesystem is null!");
            return;
        }

        sharedmount$LOGGER.debug("[cc: Shared] Successfully intercepted createFileSystem!");

        try {
            MinecraftServer server = CCSharedPlatform.INSTANCE.getCurrentServer();

            if (server != null) {
                sharedmount$LOGGER.debug("[cc: Shared] Server instance found! Creating save dir mount...");

                WritableMount sharedMount = ComputerCraftAPI.createSaveDirMount(
                        server,
                        "shared",
                        1_000_000_000L
                );

                if (sharedMount != null) {
                    filesystem.mountWritable("shared", "shared", sharedMount);
                    sharedmount$LOGGER.debug("[cc: Shared] Shared folder successfully mounted onto the computer!");
                } else {
                    sharedmount$LOGGER.debug("[cc: Shared] Failed to create SaveDirMount via ComputerCraftAPI.");
                }
            } else {
                sharedmount$LOGGER.debug("[cc: Shared] MinecraftServer is null (likely running on the client/menu context).");
            }
        } catch (Exception e) {
            sharedmount$LOGGER.error("[cc: Shared] Unexpected error occurred while mounting the shared directory:", e);
        }
    }
}
