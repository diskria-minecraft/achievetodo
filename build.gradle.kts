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
            systemProperties.putAll(
                mapOf(
                    "mixin.debug.export" to "true",
                    "mixin.debug.verify" to "true",
                    // "mixin.debug.verbose" to "true",
                    "mixin.debug.countInjections" to "true",
                    "mixin.dumpTargetOnFailure" to "true",
                    "mixin.checks.interfaces" to "true",
                )
            )

            val isLinux = System.getProperty("os.name").lowercase().contains("linux")
            val isWayland = System.getenv("XDG_SESSION_TYPE").equals("wayland", ignoreCase = true) ||
                    !System.getenv("WAYLAND_DISPLAY").isNullOrBlank()

            if (isLinux && isWayland) {
                val envMap = mutableMapOf<String, Any>(
                    "SDL_VIDEODRIVER" to "wayland",
                    "SDL_WL_CURSOR_WARP" to 0,
                )
                val isX64 = System.getProperty("os.arch").lowercase() in listOf("amd64", "x86_64")
                val nvidiaEgl = "/usr/share/glvnd/egl_vendor.d/10_nvidia.json"
                val hasNvidia = File("/proc/driver/nvidia/version").isFile && File(nvidiaEgl).isFile
                if (isX64 && hasNvidia) {
                    val nvidiaFixes = mapOf(
                        "__EGL_VENDOR_LIBRARY_FILENAMES" to nvidiaEgl,
                        "__GL_THREADED_OPTIMIZATIONS" to 0,
                        "__GLX_VENDOR_LIBRARY_NAME" to "nvidia",
                        "EGL_PLATFORM" to "wayland",
                        "GBM_BACKEND" to "nvidia-drm",
                        "MESA_LOADER_DRIVER_OVERRIDE" to "zink",
                    )
                    nvidiaFixes.forEach { (key, value) ->
                        if (System.getenv(key) == null) {
                            envMap[key] = value
                        }
                    }
                }
                environmentVars.putAll(envMap)
            }
        }
    }
    runs.all {
        runDirectory = layout.projectDirectory.dir("run/$name")
    }
}

dependencies {
    runtimeOnly("maven.modrinth:9s6osm5g:fg2uyxOW") // ClothConfigApi         | 26.3.159+fabric            | https://modrinth.com/mod/cloth-config/versions?g=26.3&l=fabric
    runtimeOnly("maven.modrinth:1eAoo2KR:s9SjoFu1") // YetAnotherConfigLib    | 3.9.7+26.3-fabric          | https://modrinth.com/mod/yacl/versions?g=26.3&l=fabric
    runtimeOnly("maven.modrinth:sodium:mc26.3-0.9.2-fabric") // Sodium                 | mc26.3-0.9.2-fabric        | https://modrinth.com/mod/sodium/versions?g=26.3&l=fabric
    runtimeOnly("maven.modrinth:sodium-extra:mc26.3-0.9.5+fabric") // Sodium Extra           | mc26.3-0.9.5+fabric        | https://modrinth.com/mod/sodium-extra/versions?g=26.3&l=fabric
    runtimeOnly("maven.modrinth:reeses-sodium-options:mc26.3-2.2.4+fabric") // Reese's Sodium Options | mc26.3-2.2.4+fabric        | https://modrinth.com/mod/reeses-sodium-options/versions?g=26.3&l=fabric
    runtimeOnly("maven.modrinth:gvQqBUqZ:fWv5bnZk") // Lithium                | mc26.3-0.26.3-fabric       | https://modrinth.com/mod/lithium/versions?g=26.3&l=fabric
    runtimeOnly("maven.modrinth:fQEb0iXm:UugdIYJw") // Krypton                | 0.3.2                      | https://modrinth.com/mod/krypton/versions?g=26.3&l=fabric
    runtimeOnly("maven.modrinth:VSNURh3q:gKKkuSYc") // C2ME                   | 0.4.2-alpha.0.106+26.3     | https://modrinth.com/mod/c2me-fabric/versions?g=26.3&l=fabric
    runtimeOnly("maven.modrinth:8shC1gFX:XB7u4eSf") // BetterF3               | 20.0.0                     | https://modrinth.com/mod/betterf3/versions?g=26.3&l=fabric
    runtimeOnly("maven.modrinth:w7ThoJFB:RUBpNu00") // Zoomify                | 2.17.0+mc26.3              | https://modrinth.com/mod/zoomify/versions?g=26.3&l=fabric
    runtimeOnly("maven.modrinth:uLbm7CG6:QVn4cccd") // Language Reload        | 1.7.8+26.3                 | https://modrinth.com/mod/language-reload/versions?g=26.3&l=fabric
    runtimeOnly("maven.modrinth:ZS0HNETF:h6gtFPqk") // Third Person Nametags  | 1.8.0                      | https://modrinth.com/mod/third-person-nametags/versions?g=26.3&l=fabric
    runtimeOnly("maven.modrinth:YL57xq9U:vTN4NRGW") // Iris Shaders           | 1.11.7+26.3-fabric         | https://modrinth.com/mod/iris/versions?g=26.3&l=fabric
    runtimeOnly("maven.modrinth:mOgUt4GM:kyy7dbrZ") // Mod Menu               | 21.0.0                     | https://modrinth.com/mod/modmenu/versions?g=26.3&l=fabric
}
