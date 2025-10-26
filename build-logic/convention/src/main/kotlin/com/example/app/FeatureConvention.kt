package com.example.app

import appIdentity
import com.example.app.convention.extensions.coreModules
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate

class FeatureConvention : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("${appIdentity.packageName}.kotlinMultiplatformLibrary")
        pluginManager.apply("${appIdentity.packageName}.composeMultiplatformPlugin")

        dependencies {
            coreModules.all.forEach {
                add("implementation", it)
            }
        }

    }
}
