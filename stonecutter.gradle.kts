plugins {
    id("dev.kikugie.stonecutter")
    id("dev.kikugie.loom-back-compat") version "0.4.2" apply false
    id("fabric-loom") version "1.18-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.147" apply false
}

stonecutter active "26.3-fabric"

stonecutter parameters {
    val loader = node.metadata.project.substringAfterLast('-')

    constants["fabric"]      = (loader == "fabric")
    constants["neoforge"]    = (loader == "neoforge")

    swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
    swaps["minecraft"]   = "\"${node.metadata.version}\";"

    replacements {
        string(current.version >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
    }
}
