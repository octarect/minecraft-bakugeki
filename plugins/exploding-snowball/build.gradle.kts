plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

fun gitVersion(): String {
    System.getenv("VERSION")?.takeIf { it.isNotEmpty() }?.let { return it }
    val tag = runCatching {
        ProcessBuilder("git", "describe", "--tags", "--exact-match")
            .start().inputStream.bufferedReader().readText().trim()
    }.getOrNull()
    if (!tag.isNullOrEmpty()) return tag
    return ProcessBuilder("git", "rev-parse", "--short", "HEAD")
        .start().inputStream.bufferedReader().readText().trim()
}

group = "io.github.octarect"
version = gitVersion()

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.shadowJar {
    archiveBaseName = "exploding-snowball"
    archiveClassifier = ""
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
