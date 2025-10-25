package com.example.app

import com.android.build.api.dsl.LibraryExtension
import com.example.app.convention.extensions.configureKotlinAndroid
import com.example.app.convention.extensions.configureKotlinMultiplatform
import com.example.app.convention.extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KotlinMultiplatformLibrary: Plugin<Project> {
    override fun apply(target: Project):Unit = with(target){
        with(pluginManager){
            listOf(
                "kotlinMultiplatform",
                "androidLibrary",
            ).forEach { id ->
                pluginManager.apply(libs.findPlugin(id).get().get().pluginId)
            }
        }

        extensions.configure<KotlinMultiplatformExtension>(::configureKotlinMultiplatform)
        extensions.configure<LibraryExtension>(::configureKotlinAndroid)
    }
}