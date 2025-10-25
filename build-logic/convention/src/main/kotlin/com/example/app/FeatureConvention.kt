package com.example.app

import com.example.app.convention.extensions.coreModules
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class FeatureConvention : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.example.app.kotlinMultiplatformLibrary")

        dependencies {
            coreModules.all.forEach {
                add("implementation", it)
            }
        }

    }
}
