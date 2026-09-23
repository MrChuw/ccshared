package com.mrchuw.ccshared;

import net.minecraft.server.MinecraftServer;

public interface CCSharedPlatform {

    //? if fabric {
    CCSharedPlatform INSTANCE = new com.mrchuw.ccshared.fabric.FabricPlatformImpl();
    //?} else if neoforge {
    /*CCSharedPlatform INSTANCE = new com.mrchuw.ccshared.neoforge.NeoforgePlatformImpl();
    *///?}

    MinecraftServer getCurrentServer();
}
