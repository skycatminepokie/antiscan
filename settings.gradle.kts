pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.8"
}

stonecutter {
    create(rootProject) {
        versions("1.21.7",
            "1.21.5",
            "1.20.6",
            "1.20.4",
            "1.20.1")
        vcsVersion = "1.21.7"
    }
}

rootProject.name = "Antiscan"