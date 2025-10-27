package com.example.app

import appIdentity
import com.example.app.extensions.configureProjectDependencies
import com.example.app.extensions.coreModules
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class DatasourceConvention : Plugin<Project>{
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("${appIdentity.packageName}.kotlinMultiplatformLibrary")

        configureProjectDependencies(coreModules.all)
    }
}