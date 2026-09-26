@file:Suppress("UnstableApiUsage")

plugins {
    id("dev.kikugie.loom-back-compat")
    id("fabric-loom")
}

version = "${property("mod.version")}+${property("deps.minecraft")}-fabric"
base.archivesName = property("mod.id") as String

val requiredJava = when {
    sc.current.parsed >= "26.1"   -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18"   -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17"   -> JavaVersion.VERSION_16
    else                          -> JavaVersion.VERSION_1_8
}

repositories {
    mavenLocal()
    maven("https://maven.squiddev.cc") { name = "SquidDev" }
    maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric-loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric-api")}")

    modImplementation("cc.tweaked:cc-tweaked-${sc.current.version}-fabric:${property("deps.cc_tweaked")}")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

loom {
    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }

//    runConfigs.named("server") {
//        preferGradleTask = true
//        generateRunConfig = true
//        runDirectory = file("run/server")
//        jvmArguments.add("-Dminecraft.eula=true")
//    }

    runConfigs.named("client") {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = file("run")
        jvmArguments.add("-Dmixin.debug.export=true")
    }
}

tasks {
    processResources {
        dependsOn("stonecutterGenerate")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        exclude("**/neoforge.mods.toml", "**/mods.toml", "**/accesstransformer.cfg")

        val props = mapOf(
            "mod_id" to project.property("mod.id") as String,
            "mod_name" to project.property("mod.name") as String,
            "mod_description" to project.property("mod.description") as String,
            "mod_version" to project.property("mod.version") as String,
            "mod_authors" to project.property("mod.authors") as String,
            "mod_repo_url" to project.property("mod.repo_url") as String,
            "mod_license" to project.property("mod.license") as String,
            "mod_logo" to project.property("mod.logo") as String,
            "fabric_version_range" to project.property("deps.fabric_version_range") as String
        )

        filesMatching(listOf("fabric.mod.json")) { expand(props) }
        filesMatching("*.mixins.json") { expand("java" to "JAVA_${requiredJava.majorVersion}") }
    }

    named("compileJava") { dependsOn("stonecutterGenerate") }
    named("compileTestJava") { dependsOn("stonecutterGenerate") }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to build/libs/{mod version}/"

        inputs.property("version", project.property("mod.version"))
        from(loomx.modJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

tasks.named("compileJava") {
    dependsOn("stonecutterGenerate")
}

tasks.named("compileTestJava") {
    dependsOn("stonecutterGenerate")
}

tasks.processResources {
    notCompatibleWithConfigurationCache("Uses Stonecutter's version properties")
}
