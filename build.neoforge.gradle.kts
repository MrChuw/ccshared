plugins {
    id("net.neoforged.moddev")
}

version = "${property("mod.version")}+${property("deps.minecraft")}-neoforge"
base.archivesName = property("mod.id") as String

val requiredJava = when {
    sc.current.parsed >= "26.1"   -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18"   -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17"   -> JavaVersion.VERSION_16
    else                          -> JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
    maven("https://maven.squiddev.cc") { name = "SquidDev" }
}

dependencies {
    implementation("cc.tweaked:cc-tweaked-${sc.current.version}-forge:${property("deps.cc_tweaked")}")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}

val modId = property("mod.id") as String

neoForge {
    version = property("deps.neoforge") as String
    validateAccessTransformers = true

    runs {
        configureEach {
            disableIdeRun()
        }

        register("client") {
            gameDirectory = file("run/")
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        // register("server") { gameDirectory = file("run/server"); server() }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }

    sourceSets["main"].resources.srcDir("src/main/generated")
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        exclude("**/fabric.mod.json", "**/*.accesswidener", "**/mods.toml")
        dependsOn("stonecutterGenerate")
    }
    named("createMinecraftArtifacts") { dependsOn("stonecutterGenerate") }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to build/libs/{mod version}/"

        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

tasks.named<ProcessResources>("processResources") {
    val props = HashMap<String, String>().apply {
        this["mod_id"] = project.property("mod.id") as String
        this["mod_name"] = project.property("mod.name") as String
        this["mod_description"] = project.property("mod.description") as String
        this["mod_version"] = project.property("mod.version") as String
        this["mod_authors"] = project.property("mod.authors") as String
        this["mod_repo_url"] = project.property("mod.repo_url") as String
        this["mod_license"] = project.property("mod.license") as String
        this["mod_logo"] = project.property("mod.logo") as String
        this["neoforge_version_range"] = project.property("deps.neoforge_version_range") as String
        this["minecraft_version_range"] = project.property("deps.minecraft_version_range") as String
    }

    filesMatching(listOf("META-INF/neoforge.mods.toml")) { expand(props) }
    filesMatching("*.mixins.json") { expand("java" to "JAVA_${requiredJava.majorVersion}") }
}

tasks.register("generateGradleIdeaRuns") {
    group = "ide"
    description = "Generates IntelliJ run configurations targeting Gradle tasks"

    val targetDir = rootProject.layout.projectDirectory.dir(".run")
    val subprojectPath = project.path
    val configName = "NeoForge ${project.name}: runClient"
    val fileName = "NeoForge_${project.name}_runClient_Gradle.run.xml"

    doLast {
        val runDir = targetDir.asFile
        runDir.mkdirs()

        val runClientXml = """
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="$configName" type="GradleRunConfiguration" factoryName="Gradle">
                <ExternalSystemSettings>
                  <option name="executionName" />
                  <option name="externalProjectPath" value="${'$'}PROJECT_DIR${'$'}" />
                  <option name="externalSystemIdString" value="GRADLE" />
                  <option name="scriptParameters" value="" />
                  <option name="taskDescriptions">
                    <list />
                  </option>
                  <option name="taskNames">
                    <list>
                      <option value="$subprojectPath:runClient" />
                    </list>
                  </option>
                  <option name="vmOptions" value="" />
                </ExternalSystemSettings>
                <ExternalSystemDebugServerProcess>true</ExternalSystemDebugServerProcess>
                <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>
                <DebugAllEnabled>false</DebugAllEnabled>
                <RunAsTest>false</RunAsTest>
                <method v="2" />
              </configuration>
            </component>
        """.trimIndent()

        File(runDir, fileName).writeText(runClientXml)
    }
}

tasks.named("compileJava") { dependsOn("stonecutterGenerate") }
tasks.named("compileTestJava") { dependsOn("stonecutterGenerate") }

tasks.processResources {
    notCompatibleWithConfigurationCache("Uses Stonecutter's version properties")
}

tasks.matching { it.name == "ideaSyncTask" }.configureEach {
    dependsOn("stonecutterGenerate")
}
