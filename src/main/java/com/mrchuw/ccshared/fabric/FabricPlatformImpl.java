package com.mrchuw.ccshared.fabric;

//? if fabric {
import com.mrchuw.ccshared.CCSharedPlatform;
import net.minecraft.server.MinecraftServer;

public class FabricPlatformImpl implements CCSharedPlatform {

    private static MinecraftServer currentServer;

    public static void setServer(MinecraftServer server) {
        currentServer = server;
    }

    @Override
    public MinecraftServer getCurrentServer() {
        return currentServer;
    }
}
//?}