import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

plugins {
    id("onlyshots.kmp.library")
    id("onlyshots.kmp.wasm")
}

/** JavaCPP's name for the OS and CPU this build runs on, which picks the FFmpeg natives the desktop build carries. */
val desktopNatives: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arm = System.getProperty("os.arch").lowercase().let { it.contains("aarch64") || it.contains("arm64") }
    when {
        os.contains("mac") -> if (arm) "macosx-arm64" else "macosx-x86_64"
        os.contains("win") -> "windows-x86_64"
        else -> if (arm) "linux-arm64" else "linux-x86_64"
    }
}

/** Compose Desktop's name for this OS and CPU. */
val composeOs: String =
    desktopNatives.replace("macosx", "macos").replace("x86_64", "x64").replace("linux-arm64", "linux-arm64")

kotlin {
    extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
        // The device test reads the spike clip from its own assets.
        androidResources { enable = true }
        // The export alignment test runs on a device; it reads the spike clip from the app's
        // resources as an asset.
        withDeviceTest { instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    }

    // Skia frame drawing (skikoMain) is shared by the exporters that draw frames on the CPU: iOS, the desktop editor
    // and the web (wasm and JS). Added to the default hierarchy rather than wired by hand, which would switch the
    // default off.
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("skiko") {
                group("ios") { withIos() }
                withJvm()
                group("web") {
                    withJs()
                    withWasmJs()
                }
            }
        }
    }

    sourceSets {
        // The spike clip fixture is shared by the common tests and the on-device export test.
        commonTest.get().kotlin.srcDir("src/testFixtures/kotlin")
        getByName("androidDeviceTest").kotlin.srcDir("src/testFixtures/kotlin")

        commonMain.dependencies {
            api(projects.core.model)
            api(projects.overlay.compose)
            api(libs.kotlinx.coroutines.core)
            api(libs.okio)
        }
        androidMain.dependencies {
            implementation(libs.androidx.media3.transformer)
            implementation(libs.androidx.media3.effect)
            implementation(libs.androidx.media3.exoplayer)
        }
        getByName("desktopTest").dependencies {
            implementation(libs.kotlinx.coroutines.test)
            // Skia's natives for this OS, as compose.desktop.currentOs gives the app (this module has no Compose
            // plugin): the exporter draws with Skia.
            implementation(
                "org.jetbrains.compose.desktop:desktop-jvm-$composeOs:${libs.versions.compose.multiplatform.get()}"
            )
        }
        getByName("desktopMain").dependencies {
            // FFmpeg for the desktop editor's playback, probe and export: Java bindings for every OS, natives (the GPL
            // build, for libx264) for this one only, so a desktop build carries one OS's FFmpeg, not all of them.
            implementation("org.bytedeco:javacv:${libs.versions.javacv.asProvider().get()}") {
                // Only FFmpeg is used: not OpenCV, its BLAS, or the camera and depth-sensor SDKs JavaCV also wraps.
                for (unused in
                    listOf(
                        "opencv",
                        "openblas",
                        "flycapture",
                        "libdc1394",
                        "libfreenect",
                        "libfreenect2",
                        "librealsense",
                        "librealsense2",
                        "videoinput",
                        "artoolkitplus",
                        "leptonica",
                        "tesseract",
                    )) exclude(group = "org.bytedeco", module = unused)
            }
            implementation("org.bytedeco:ffmpeg:${libs.versions.javacv.ffmpeg.get()}:$desktopNatives-gpl")
            implementation("org.bytedeco:javacpp:${libs.versions.javacv.asProvider().get()}:$desktopNatives")
        }
        webMain.dependencies {
            implementation(libs.okio.fakefilesystem)
            // The one export failure the browser can explain to the user (no WebGL), in words.
            implementation(projects.core.resources)
        }
        commonTest.dependencies { implementation(libs.kotlinx.coroutines.test) }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.junit)
        }
    }
}

/**
 * The device test reads the spike clip as an asset. The clip itself lives once, in video/testdata; this copies it into
 * the device-test assets, which are git-ignored.
 */
val copySpikeClip by
    tasks.registering(Copy::class) {
        from(layout.projectDirectory.file("testdata/spike_alignment.mp4").asFile)
        into(layout.projectDirectory.dir("src/androidDeviceTest/assets"))
    }

/**
 * The exporters read their overlay labels from :core:resources (overlayLabels()), but nothing packages those Compose
 * resources into this module's test binaries: AGP 9's KMP library plugin leaves them out of the device-test APK, and
 * without the Compose Gradle plugin nothing copies them beside the iOS test executable. Every export test failed with
 * MissingResourceException. Copy them where the runtime reader looks (as app/androidApp does for the app).
 */
val coreComposeResources =
    project(":core:resources")
        .layout
        .buildDirectory
        .dir("generated/compose/resourceGenerator/preparedResources/commonMain/composeResources")
val coreResourcePath = "composeResources/app.onlyshots.core.resources"

val copyCoreComposeResources by
    tasks.registering(Sync::class) {
        from(coreComposeResources)
        into(layout.projectDirectory.dir("src/androidDeviceTest/assets/$coreResourcePath"))
        dependsOn(":core:resources:prepareComposeResourcesTaskForCommonMain")
    }

// iOS reads them from compose-resources/ beside the test executable.
val copyCoreComposeResourcesForIosTest by
    tasks.registering(Sync::class) {
        from(coreComposeResources)
        into(layout.buildDirectory.dir("bin/iosSimulatorArm64/debugTest/compose-resources/$coreResourcePath"))
        dependsOn(":core:resources:prepareComposeResourcesTaskForCommonMain")
    }

tasks
    .matching { it.name == "mergeAndroidDeviceTestAssets" || it.name == "generateAndroidDeviceTestAssets" }
    .configureEach { dependsOn(copySpikeClip, copyCoreComposeResources) }

// The iOS export test reads the spike clip from the repository (simctl passes SIMCTL_CHILD_*).
tasks.withType<KotlinNativeSimulatorTest>().configureEach {
    dependsOn(copyCoreComposeResourcesForIosTest)
    environment(
        "SIMCTL_CHILD_ONLYSHOTS_SPIKE_CLIP",
        layout.projectDirectory.file("testdata/spike_alignment.mp4").asFile.absolutePath,
    )
}

// The desktop export test reads the spike clip from the repository. HeaderMasterTest, which renders the App Store
// header's master through the same exporter, is handed its input from the command line instead and skips without it
// (design/header/README.md); -D on the Gradle command line reaches the Gradle JVM, not this one, so pass it through.
tasks
    .withType<Test>()
    .matching { it.name == "desktopTest" }
    .configureEach {
        systemProperty(
            "onlyshots.spikeClip",
            layout.projectDirectory.file("testdata/spike_alignment.mp4").asFile.absolutePath,
        )
        for (name in listOf("video", "shot", "out", "shotStartUs", "durationUs", "rect", "title")) {
            providers.systemProperty("onlyshots.header.$name").orNull?.let {
                systemProperty("onlyshots.header.$name", it)
            }
        }
    }
