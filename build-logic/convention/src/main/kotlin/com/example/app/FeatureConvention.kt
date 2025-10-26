package com.example.app

import appIdentity
import com.example.app.extensions.coreModules
import com.example.app.extensions.datasourceModules
import com.example.app.extensions.presentationModules
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class FeatureConvention : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("${appIdentity.packageName}.kotlinMultiplatformLibrary")
        pluginManager.apply("${appIdentity.packageName}.composeMultiplatformPlugin")

        dependencies {
            coreModules.all.forEach {
                add("implementation", it)
            }
            datasourceModules.all.forEach {
                add("implementation", it)
            }
            presentationModules.all.forEach {
                add("implementation", it)
            }
        }

    }
}
