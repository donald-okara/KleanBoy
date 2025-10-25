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
    val moduleName = path.split(":").drop(1).joinToString(".")

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

val Project.coreModules: CoreModules
    get() = CoreModules(this)

/**
 * Represents all core modules in the project hierarchy.
 */
class CoreModules(private val project: Project) {
    val all: List<Project> = emptyList() //TODO: Remove this
//    val datasource get() = project.project(":core:datasource")
//    val domain get() = project.project(":core:domain")
//    val ui get() = project.project(":core:ui")
//    val network get() = project.project(":core:network")

    /** Returns all core modules as a list. */
//    val all get() = listOf(datasource, domain, ui, network) // TODO: Replace with this
}

val Project.featureModules: FeatureModules
    get() = FeatureModules(this)

class FeatureModules(private val project: Project) {
    val all: List<Project> = emptyList() //TODO: Remove this
    //    val authentication get() = project.project(":feature:authentication")
//    val home get() = project.project(":feature:home")

    /** Returns all feature modules as a list. */
//    val all get() = listOf(authentication, home) //TODO: Replace with this
}