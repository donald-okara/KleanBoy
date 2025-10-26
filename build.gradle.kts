plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
}

// In the root build.gradle.kts (or a common module)
val updateAppConfig by tasks.registering(Exec::class) {
    group = "project setup"
    description = "Runs the updateAppConfig.kts script to refactor package/app names (manual, use at your own risk)"

    // Path to your script
    val scriptFile = file("updateAppConfig.kts")

    // You can pass args via -P flags or hardcode defaults
    val packagePrefix = project.findProperty("packagePrefix")?.toString()
        ?: throw GradleException("Missing -PpackagePrefix")
    val packageName = project.findProperty("packageName")?.toString()
        ?: throw GradleException("Missing -PpackageName")
    val appName = project.findProperty("appName")?.toString()
        ?: throw GradleException("Missing -PappName")

    // Command line to run kotlinc script
    commandLine(
        "kotlinc",
        "-script",
        scriptFile.absolutePath,
        packagePrefix,
        packageName,
        appName
    )

    // Optional: fail the task if the script fails
    isIgnoreExitValue = false
}
