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
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven") { name = "Modrinth" }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

// Local Dev Env
loom {
    runs {
        getByName("client") {
            jvmArguments.addAll(
                "-Xmx24G",
                "-Xms24G",
                "-XX:+UseG1GC",
                "-XX:+ParallelRefProcEnabled",
                "-XX:MaxGCPauseMillis=200",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+AlwaysPreTouch",
                "-XX:G1NewSizePercent=30",
                "-XX:G1MaxNewSizePercent=40",
                "-XX:G1HeapRegionSize=8m",
                "-XX:G1ReservePercent=15",
                "-XX:G1HeapWastePercent=5",
                "-XX:G1MixedGCCountTarget=4",
                "-XX:InitiatingHeapOccupancyPercent=15",
                "-XX:G1MixedGCLiveThresholdPercent=90",
                "-XX:SurvivorRatio=32",
                "-XX:+PerfDisableSharedMem",
                "-XX:MaxTenuringThreshold=1",
                "--enable-native-access=ALL-UNNAMED",
            )
            environmentVars.putAll(mapOf(
                "__NV_PRIME_RENDER_OFFLOAD" to "1",
                "__GLX_VENDOR_LIBRARY_NAME" to "nvidia",
                "ENABLE_VKB_WSI_LAYER" to "1",
            ))
            systemProperties.putAll(mapOf(
                "sun.graphics.cdw" to "true",
            ))
        }
    }
}

dependencies {
    runtimeOnly("maven.modrinth:9s6osm5g:fg2uyxOW") // ClothConfigApi           https://modrinth.com/mod/cloth-config/version/26.3.159+fabric
    runtimeOnly("maven.modrinth:AANobbMI:g6ju16VJ") // Sodium                   https://modrinth.com/mod/sodium/version/mc26.3-0.9.3-beta.1-fabric
    runtimeOnly("maven.modrinth:gvQqBUqZ:fWv5bnZk") // Lithium                  https://modrinth.com/mod/lithium/version/mc26.3-0.26.3-fabric
    runtimeOnly("maven.modrinth:fQEb0iXm:UugdIYJw") // Krypton                  https://modrinth.com/mod/krypton/version/0.3.2
    runtimeOnly("maven.modrinth:VSNURh3q:gKKkuSYc") // C2ME                     https://modrinth.com/mod/c2me-fabric/version/0.4.2-alpha.0.106+26.3
    runtimeOnly("maven.modrinth:8shC1gFX:XB7u4eSf") // BetterF3                 https://modrinth.com/mod/betterf3/version/20.0.0
    runtimeOnly("maven.modrinth:aXf2OSFU:g9ICu3fu") // OkZoomer                 https://modrinth.com/mod/betterf3/version/20.0.0
    runtimeOnly("maven.modrinth:mOgUt4GM:kyy7dbrZ") // Mod Menu                 https://modrinth.com/mod/modmenu/version/21.0.0
}
