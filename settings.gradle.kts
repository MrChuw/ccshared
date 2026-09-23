pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
        maven("https://maven.squiddev.cc") { name = "SquidDev" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        fun match(version: String, vararg loaders: String) = loaders
            .forEach { version("$version-$it", version).buildscript = "build.$it.gradle.kts" }

        match("1.21.1", "fabric", "neoforge")
        match("1.21.1", "fabric", "neoforge")
        //match("1.21.10", "fabric", "neoforge")
        match("1.21.11", "fabric", "neoforge")
        // match("26.1",   "fabric", "neoforge")
        // match("26.1.1", "fabric", "neoforge")
        match("26.1.2", "fabric", "neoforge")
        match("26.2",   "fabric", "neoforge")
        match("26.3",   "fabric", "neoforge")

        vcsVersion = "26.3-neoforge"
    }
}

rootProject.name = "cc Shared"
