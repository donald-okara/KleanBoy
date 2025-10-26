package com.example.app.convention.extensions

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

val Project.coreModules: CoreModules
    get() = CoreModules(this)

/**
 * Represents all core modules in the project hierarchy.
 */
class CoreModules(private val project: Project) {
    val domain get() = project.project(":core:domain")
    val utils get() = project.project(":core:utils")

    val all get() = listOf(domain, utils)
}

val Project.featureModules: FeatureModules
    get() = FeatureModules(this)

class FeatureModules(private val project: Project) {
    val authentication get() = project.project(":feature:authentication")

    val all get() = listOf(authentication)
}

val Project.presentationModules: PresentationModules
    get() = PresentationModules(this)

class PresentationModules(private val project: Project) {
    val design get() = project.project(":presentation:design")
    val resources get() = project.project(":presentation:resources")
    val common get() = project.project(":presentation:common")

    val all get() = listOf(design, resources, common)
}

val Project.datasourceModules: DatasourceModules
    get() = DatasourceModules(this)

class DatasourceModules(private val project: Project) {
    val remote get() = project.project(":datasource:remote")
    val local get() = project.project(":datasource:local")

    val all get() = listOf(remote, local)
}