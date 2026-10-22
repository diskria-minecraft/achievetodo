pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net") { name = "Fabric" }
        maven("https://central.sonatype.com/repository/maven-snapshots") // TODO: Required for Lapis KSP.
    }
}
