import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.lapis)
}

val namespace = "io.github.diskria"
val modId = "achievetodo"
val modPackage = "$namespace.$modId"

group = namespace
version = "2.0.0"

loom {
    accessWidenerPath = file("src/main/resources/$modId.classtweaker")
}

fabricApi {
    configureDataGeneration()
}

lapis {
    uniqueModPrefix = "$modId$"
    mixinConfig = file("src/main/resources/$modId.mixins.json")
}

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.kotlin)
    implementation(libs.fabric.api)

    implementation(libs.toml)
}

tasks.withType<Jar> {
    exclude("**/*.kotlin_module")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

sourceSets {
    main {
        resources.srcDir("src/main/generated")
        java.srcDir("src/main/generated/java")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenLocal()
    mavenCentral()

    // TODO: Required for Lapis KSP.
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}
