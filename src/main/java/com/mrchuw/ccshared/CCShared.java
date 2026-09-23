package com.mrchuw.ccshared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CCShared {

    public static final String MOD_ID = "ccshared";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final String VERSION   = /*$ mod_version*/ "1.0.0";
    public static final String MINECRAFT = /*$ minecraft*/   "26.3";

    public static void init() {
        LOGGER.info("Initializing {} {} for Minecraft {}", MOD_ID, VERSION, MINECRAFT);
    }
}