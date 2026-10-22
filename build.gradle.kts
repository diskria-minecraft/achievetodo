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
    runs {
        register("datagen") {
            name = "Data Generation"

            inherit(getByName("server"))

            vmArg("-Dfabric-api.datagen")
            vmArg("-Dfabric-api.datagen.output-dir=${layout.projectDirectory.dir("src/main/generated").asFile.absolutePath}")
            vmArg("-Dfabric-api.datagen.modid=$modId")

            runDir = "build/datagen"
        }
    }
}

lapis {
    uniqueModPrefix = "$modId$"
    mixinConfig = file("src/main/resources/$modId.mixins.json")
}

dependencies {
    minecraft(libs.minecraft)
    mappings(variantOf(libs.fabric.yarn) { classifier("v2") })

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.kotlin)
    modImplementation(libs.fabric.api)

    modImplementation(libs.toml)
}

tasks.withType<Jar> {
    exclude("**/*.kotlin_module")

    exclude(".cache/**")

}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

tasks.processResources {
    doFirst {
        file("build/datagen").mkdirs()
    }
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
