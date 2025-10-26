package com.example.app.convention.extensions

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import kotlin.jvm.kotlin

internal fun Project.configureKotlinMultiplatform(
    extension: KotlinMultiplatformExtension
) = extension.apply {
    jvmToolchain(17)

    // targets
    androidTarget()
    jvm()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = moduleName.replace(".", "_") + "_Kit"
            isStatic = true
        }
    }

    applyDefaultHierarchyTemplate()

    //common dependencies
    sourceSets.apply {
        commonMain {
            dependencies {
            }
        }

        androidMain {
            dependencies {
            }
        }

        commonTest.dependencies {
            implementation(libs.findLibrary("kotlin.test").get())
        }
        jvmMain.dependencies {
            implementation(libs.findLibrary("kotlinx.coroutinesSwing").get())
        }
    }

}