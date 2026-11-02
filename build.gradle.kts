import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.lapis)
}

val namespace = "io.github.diskria"
val modId = "achievetodo"
val modPackage = "$namespace.$modId"

group = namespace
version = "3.0.0"

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
}

tasks.withType<Jar> {
    exclude("**/*.kotlin_module")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

repositories {
    mavenLocal()
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven") { name = "Modrinth" }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }

    // TODO: Required for Lapis KSP.
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

// Local Dev Env
loom {
    runs {
        getByName("client") {
            jvmArguments.addAll(
                "-Xms6G", "-Xmx12G",
                "-XX:+UseZGC",
                "-XX:+PerfDisableSharedMem",
                "-XX:+AlwaysPreTouch",

                "--enable-native-access=ALL-UNNAMED",
            )
            systemProperties.put("mixin.debug.export", "true")
        }
    }
    runs.all {
        runDirectory = layout.projectDirectory.dir("run/$name")
    }
}

dependencies {
    runtimeOnly("maven.modrinth:9s6osm5g:fg2uyxOW") // ClothConfigApi           https://modrinth.com/mod/cloth-config/version/26.3.159+fabric
    runtimeOnly("maven.modrinth:1eAoo2KR:s9SjoFu1") // YetAnotherConfigLib      https://modrinth.com/mod/yacl/version/3.9.7+26.3-fabric
    runtimeOnly("maven.modrinth:AANobbMI:g6ju16VJ") // Sodium                   https://modrinth.com/mod/sodium/version/mc26.3-0.9.3-beta.1-fabric
    runtimeOnly("maven.modrinth:gvQqBUqZ:fWv5bnZk") // Lithium                  https://modrinth.com/mod/lithium/version/mc26.3-0.26.3-fabric
    runtimeOnly("maven.modrinth:fQEb0iXm:UugdIYJw") // Krypton                  https://modrinth.com/mod/krypton/version/0.3.2
    runtimeOnly("maven.modrinth:VSNURh3q:gKKkuSYc") // C2ME                     https://modrinth.com/mod/c2me-fabric/version/0.4.2-alpha.0.106+26.3
    runtimeOnly("maven.modrinth:8shC1gFX:XB7u4eSf") // BetterF3                 https://modrinth.com/mod/betterf3/version/20.0.0
    runtimeOnly("maven.modrinth:w7ThoJFB:RUBpNu00") // Zoomify                  https://modrinth.com/mod/zoomify/version/2.17.0+mc26.3
    runtimeOnly("maven.modrinth:mOgUt4GM:kyy7dbrZ") // Mod Menu                 https://modrinth.com/mod/modmenu/version/21.0.0
    runtimeOnly("maven.modrinth:7BgsROit:c9u0nVfv") // Advancements Fullscreen  TODO: Embed
    runtimeOnly("maven.modrinth:Ccho67RI:mFc7amLp") // Advancements Search      TODO: Embed
}
