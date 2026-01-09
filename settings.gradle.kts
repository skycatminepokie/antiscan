pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.8.2"
}

stonecutter {
    create(rootProject) {
        versions("1.21.11",
            "1.21.9",
            "1.21.7",
            "1.21.5",
            "1.20.5")
        vcsVersion = "1.21.11"
    }
}

rootProject.name = "Antiscan"