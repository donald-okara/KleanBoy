import java.io.File

if (args.size < 3) {
    println("Usage: updateAppConfig.kts <packagePrefix> <packageName> <appName>")
    System.exit(1)
}

val (newPackagePrefix, newPackageName, newAppName) = args.toList()
val projectRoot = File(".")
val gradlePropertiesFile = File(projectRoot, "gradle.properties")
val stringsFile = File(projectRoot, "composeApp/src/main/res/values/strings.xml")

// --- Load old values from gradle.properties ---
val oldValues = mutableMapOf<String, String>()
if (gradlePropertiesFile.exists()) {
    gradlePropertiesFile.readLines().forEach { line ->
        when {
            line.startsWith("appPackagePrefix=") -> oldValues["packagePrefix"] = line.substringAfter("=")
            line.startsWith("appPackageName=") -> oldValues["packageName"] = line.substringAfter("=")
            line.startsWith("appName=") -> oldValues["appName"] = line.substringAfter("=")
        }
    }
}

// --- 1. Update gradle.properties ---
if (gradlePropertiesFile.exists()) {
    val updatedProperties = gradlePropertiesFile.readLines().map { line ->
        when {
            line.startsWith("appPackagePrefix=") -> "appPackagePrefix=$newPackagePrefix"
            line.startsWith("appPackageName=") -> "appPackageName=$newPackageName"
            line.startsWith("appName=") -> "appName=$newAppName"
            else -> line
        }
    }
    gradlePropertiesFile.writeText(updatedProperties.joinToString("\n"))
    println("Updated gradle.properties")
}

// --- 2. Update Compose strings.xml ---
if (stringsFile.exists()) {
    val updatedStrings = stringsFile.readLines().map { line ->
        if (line.contains("<string name=\"app_name\">")) {
            line.replace(Regex(">.*<"), ">$newAppName<")
        } else line
    }
    stringsFile.writeText(updatedStrings.joinToString("\n"))
    println("Updated Compose strings.xml")
}

// --- 3. Scan .kt/.kts files ---
fun replaceInFile(file: File) {
    var text = file.readText()
    oldValues.forEach { (key, oldValue) ->
        val replacement = when (key) {
            "packagePrefix" -> newPackagePrefix
            "packageName" -> newPackageName
            "appName" -> newAppName
            else -> oldValue
        }
        text = text.replace(oldValue, replacement)
    }
    file.writeText(text)
}

fun scanAndReplace(dir: File) {
    dir.listFiles()?.forEach { file ->
        if (file.isDirectory) scanAndReplace(file)
        else if (file.extension in listOf("kt", "kts")) replaceInFile(file)
    }
}

// --- 4. Rename package directories properly ---
fun renamePackageDirs(srcDir: File, oldPackage: String, newPackage: String) {
    val oldParts = oldPackage.split(".")
    val newParts = newPackage.split(".")

    // Start at srcDir, descend old package path
    var currentDir = srcDir
    for (part in oldParts) {
        val next = File(currentDir, part)
        if (!next.exists()) return
        currentDir = next
    }

    // Rebuild new path
    var targetDir = srcDir
    newParts.forEach { part -> targetDir = File(targetDir, part) }
    if (!targetDir.exists()) targetDir.mkdirs()

    // Move all files
    currentDir.listFiles()?.forEach { it.renameTo(File(targetDir, it.name)) }

    // Remove old directories recursively if empty
    var dirToDelete: File? = currentDir
    while (dirToDelete != null && dirToDelete.exists() && dirToDelete.listFiles()?.isEmpty() == true && dirToDelete != srcDir) {
        val parent = dirToDelete.parentFile
        dirToDelete.delete()
        dirToDelete = parent
    }

    println("Renamed package dirs: ${currentDir.path} -> ${targetDir.path}")
}

// --- Run everything ---
scanAndReplace(projectRoot)

// Common source dirs in Android/Kotlin projects
val srcDirs = listOf(
    File(projectRoot, "composeApp/src/main/kotlin"),
    File(projectRoot, "composeApp/src/main/java")
)
val oldPackageName = oldValues["packageName"]
if (oldPackageName != null) {
    srcDirs.forEach { renamePackageDirs(it, oldPackageName, newPackageName) }
}

println("Refactoring completed successfully.")
println("New values: packagePrefix=$newPackagePrefix, packageName=$newPackageName, appName=$newAppName")
