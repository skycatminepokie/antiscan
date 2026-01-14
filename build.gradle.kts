import org.gradle.kotlin.dsl.stonecutter

plugins {
    id("net.fabricmc.fabric-loom-remap")
//    `maven-publish`
    id("me.modmuss50.mod-publish-plugin")
}

version = "${property("mod.version")}+${stonecutter.current.version}"
base.archivesName = property("mod.id") as String
val fabricModules = arrayOf(
    "fabric-lifecycle-events-v1",
    "fabric-data-generation-api-v1",
    "fabric-gametest-api-v1",
    "fabric-command-api-v2"
)
val requiredJava =
    when {
        stonecutter.current.parsed > "1.21.11" -> JavaVersion.VERSION_25
        stonecutter.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
        stonecutter.current.parsed >= "1.17" -> JavaVersion.VERSION_17
        else -> JavaVersion.VERSION_1_8
    }

repositories {
    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    mavenCentral()
}

dependencies {

    minecraft("com.mojang:minecraft:${stonecutter.current.version}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    /**
     * Fetches only the required Fabric API modules to not waste time downloading all of them for each version.
     * @see <a href="https://github.com/FabricMC/fabric">List of Fabric API modules</a>
     */
    for (it in fabricModules) modImplementation(fabricApi.module(it, property("deps.fabric_api") as String))
    "me.lucko:fabric-permissions-api:${property("deps.permissions_api")}".let {
        include(it) {
            isTransitive = false
        }
        modImplementation(it) {
            isTransitive = false
        }
    }
    testImplementation("net.fabricmc:fabric-loader-junit:${property("deps.fabric_loader")}")

    "org.jspecify:jspecify:${property("deps.jspecify")}".let {
        compileOnly(it)
        testCompileOnly(it)
    }
    testImplementation("org.mockito:mockito-core:${property("deps.mockito")}")
    testImplementation("org.assertj:assertj-core:${property("deps.assertj")}")
}

loom {
    splitEnvironmentSourceSets()

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDir = "../../run" // Shares the run directory between versions
    }

    mods {
        create(project.property("mod.id") as String) {
            sourceSet("main")
            sourceSet("client")
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
}

tasks {
    withType<ProcessResources>().configureEach {
        inputs.property("id", project.property("mod.id"))
        inputs.property("test_id", project.property("mod.test_id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("version", "${project.property("mod.version")}+${project.property("mod.mc_title")}")
        inputs.property("minecraft", project.property("mod.mc_dep"))
        inputs.property("fabric_loader", project.property("deps.fabric_loader"))
        inputs.property("java", requiredJava)
        inputs.property("fabricModules", fabricModules)

        val javaAndInjected =
            StringBuilder(requiredJava.majorVersion.toString()).apply {
                var toDepend = fabricModules.filter {
                    !it.contains("gametest")
                }
                if (toDepend.isNotEmpty()) {
                    append('"')
                    for (module in toDepend) {
                        append(",\n    \"$module\": \"*\"")
                    }
                    delete(length - 1, length)
                }
            }.toString()

        val props = mapOf(
            "id" to project.property("mod.id"),
            "test_id" to project.property("mod.test_id"),
            "name" to project.property("mod.id"),
            "version" to "${project.property("mod.version")}+${project.property("mod.mc_title")}",
            "minecraft" to project.property("mod.mc_dep"),
            "fabric_loader" to project.property("deps.fabric_loader"),
            "java" to javaAndInjected
        )

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile }, remapSourcesJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }

    test {
        useJUnitPlatform()
    }
}

fabricApi {
    configureDataGeneration {
        client = true
    }

    @Suppress("UnstableApiUsage")
    configureTests {
        createSourceSet = true
        modId = "${project.property("mod.id")}-test"
        eula = true
    }
}

publishMods {
    file = tasks.remapJar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.remapSourcesJar.map { it.archiveFile.get() })
    displayName = "${property("mod.name")} ${property("mod.version")} for ${property("mod.mc_title")}"
    version = "${property("mod.version")}+${property("mod.mc_title")}"
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add("fabric")

    dryRun = providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null ||
            providers.environmentVariable("GITHUB_TOKEN").getOrNull() == null ||
            providers.environmentVariable("DISCORD_WEBHOOK").getOrNull() == null

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.addAll(property("mod.mc_targets").toString().split(' '))
        requires {
            slug = "fabric-api"
        }
        projectDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText
    }

    github {
        accessToken = providers.environmentVariable("GITHUB_TOKEN")
        repository = property("publish.github") as String
        commitish = providers.environmentVariable("GITHUB_REF_NAME").getOrElse("master")
        tagName = "${property("mod.version")}+${property("mod.mc_title")}"
    }

    discord {
        username = "Mod Updates"
        avatarUrl = "https://cataas.com/cat?type=square"
        webhookUrl = providers.environmentVariable("DISCORD_WEBHOOK")
        content = "# A new version of Antiscan is out!\n${changelog.get()}"
    }

}
/*
publishing {
    repositories {
        maven("...") {
            name = "..."
            credentials(PasswordCredentials::class.java)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "${property("mod.group")}.${property("mod.id")}"
            artifactId = property("mod.version") as String
            version = stonecutter.current.version

            from(components["java"])
        }
    }
}
*/