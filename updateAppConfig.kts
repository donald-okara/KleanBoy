#!/usr/bin/env kotlin
import java.io.File
import kotlin.system.exitProcess

// --- rest of your existing script ---
val newPackagePrefix = args[0]
val newPackageName = args[1]
val newAppName = args[2]

println("[WARNING] This script will refactor package names to: \n   \"$newPackagePrefix.$newPackageName\" \nAnd app name to \n  \"$newAppName\"")
println("[INFO] Make sure you are on a safe branch and have committed your work.")
println("[INFO] Enter 'yes' to continue, anything else to abort:")
val confirmation = readLine()?.trim()?.lowercase()
if (confirmation != "yes") {
    println("[ABORT] Refactoring cancelled by user.")
    exitProcess(0)
}

if (args.size < 3) {
    println("Usage: updateAppConfig.kts <packagePrefix> <packageName> <appName>")
    exitProcess(1)
}

val projectRoot: File = File(".").absoluteFile
val gradlePropertiesFile = File(projectRoot, "gradle.properties")
val stringsFile = File(projectRoot, "composeApp/src/main/res/values/strings.xml")

fun log(s: String) = println("[INFO] $s")
fun warn(s: String) = println("[WARN] $s")

// --- 1. Detect old full package ---
fun findFirstPackage(root: File): String? {
    val exts = setOf("kt","kts","java")
    root.walkTopDown()
        .filter { it.isFile && it.extension.lowercase() in exts }
        .forEach { f ->
            val regex = Regex("^\\s*package\\s+([A-Za-z0-9_.]+)", RegexOption.MULTILINE)
            regex.find(f.readText())?.let { return it.groupValues[1] }
        }
    return null
}

val oldFullPackage = findFirstPackage(projectRoot) ?: run {
    warn("Could not detect old package automatically; replacements may fail")
    ""
}

val oldParts = oldFullPackage.split(".")
val oldPackagePrefix = if (oldParts.size > 1) oldParts.dropLast(1).joinToString(".") else ""
val oldPackageName = oldParts.lastOrNull() ?: ""

val newFullPackage = if (newPackagePrefix.isNotEmpty()) "$newPackagePrefix.$newPackageName" else newPackageName

log("Old full package: '$oldFullPackage'")
log("New full package: '$newFullPackage'")

// --- 2. Update gradle.properties ---
val lines = if (gradlePropertiesFile.exists()) gradlePropertiesFile.readLines().toMutableList() else mutableListOf()
fun upsert(key: String, value: String) {
    val idx = lines.indexOfFirst { it.startsWith("$key=") }
    val line = "$key=$value"
    if (idx >= 0) lines[idx] = line else lines.add(line)
}
upsert("appPackagePrefix", newPackagePrefix)
upsert("appPackageName", newPackageName)
upsert("appName", newAppName)
gradlePropertiesFile.writeText(lines.joinToString("\n"))
log("Updated gradle.properties")

// --- 3. Update strings.xml safely ---
if (stringsFile.exists()) {
    val xmlText = stringsFile.readText()
    val pattern = Regex("(?s)<string\\s+name\\s*=\\s*\"app_name\"\\s*>.*?</string>")
    val replacement = "<string name=\"app_name\">${Regex.escapeReplacement(newAppName)}</string>"
    stringsFile.writeText(pattern.replace(xmlText, replacement))
    log("Updated strings.xml")
}

// --- 4. Build replacements map ---
val replacements = linkedMapOf<String,String>()
if (oldFullPackage.isNotBlank()) replacements[oldFullPackage] = newFullPackage
if (oldPackagePrefix.isNotBlank()) replacements[oldPackagePrefix] = newPackagePrefix
if (oldPackageName.isNotBlank()) replacements[oldPackageName] = newPackageName

// --- 5. Replace text safely in source files ---
val allowedExt = setOf("kt","kts","java","xml","gradle","properties","mf","json","md","txt","yaml","yml","pro")
val excludedDirs = setOf("build",".gradle",".git","node_modules","out")

fun isExcluded(f: File): Boolean {
    var cur: File? = f
    while (cur != null && cur.absolutePath.startsWith(projectRoot.absolutePath)) {
        if (excludedDirs.contains(cur.name)) return true
        cur = cur.parentFile
    }
    return false
}

fun replaceInFile(file: File) {
    val text = file.readText()
    var newText = text

    // Replace full package everywhere (safe)
    if (oldFullPackage.isNotBlank()) newText = newText.replace(oldFullPackage, newFullPackage)

    // Replace package prefix only in package/import lines
    if (oldPackagePrefix.isNotBlank()) {
        val regex = Regex("(?m)^(\\s*(package|import)\\s+)${Regex.escape(oldPackagePrefix)}")
        newText = regex.replace(newText) { it.groupValues[1] + newPackagePrefix }
    }

    // Replace package name only in package/import lines with word boundary
    if (oldPackageName.isNotBlank()) {
        val regex = Regex("(?m)^(\\s*(package|import)\\s+.*\\.)${Regex.escape(oldPackageName)}\\b")
        newText = regex.replace(newText) { it.groupValues[1] + newPackageName }
    }

    // Replace appName only in safe XML/Gradle/Manifest files
    if (file.extension in setOf("xml","gradle","properties","mf")) {
        newText = newText.replace(Regex("\\b${Regex.escape(oldPackageName)}\\b"), newPackageName)
        newText = newText.replace(Regex("\\b${Regex.escape(oldFullPackage)}\\b"), newFullPackage)
    }

    if (newText != text) file.writeText(newText)
}

fun scanAndReplace(root: File) {
    root.walkTopDown()
        .filter { it.isFile && allowedExt.contains(it.extension.lowercase()) && !isExcluded(it) }
        .forEach { replaceInFile(it) }
}

log("Replacing package/app references safely...")
scanAndReplace(projectRoot)
log("Text replacements complete.")

// --- 6. Move package directories safely (multi-module + nested) ---
fun movePackageDirs(srcDir: File, oldFull: String, newFull: String) {
    if (oldFull.isBlank()) return
    val oldParts = oldFull.split(".").filter { it.isNotBlank() }
    val newParts = newFull.split(".").filter { it.isNotBlank() }

    val candidateDirs = srcDir.walkTopDown()
        .filter { it.isDirectory && it.relativeTo(srcDir).invariantSeparatorsPath.endsWith(oldParts.joinToString("/")) }
        .toList()

    candidateDirs.forEach { oldDir ->
        val newDir = File(srcDir, newParts.joinToString(File.separator))
        if (!newDir.exists()) newDir.mkdirs()
        oldDir.walkTopDown().forEach { f ->
            if (f.isFile) {
                val rel = f.relativeTo(oldDir).path
                val targetFile = File(newDir, rel)
                targetFile.parentFile.mkdirs()
                f.renameTo(targetFile)
            }
        }
        // Delete empty old dirs recursively
        var cur: File? = oldDir
        while (cur != null && cur.exists() && cur.listFiles()?.isEmpty() == true) {
            val parent = cur.parentFile
            cur.delete()
            cur = parent
        }
        log("Moved package dir: ${oldDir.path} -> ${newDir.path}")
    }
}

// --- Multi-module aware scan for all src/kotlin dirs ---
projectRoot.walkTopDown()
    .filter { it.isDirectory && it.name == "kotlin" && it.parentFile?.name in setOf("main","test") }
    .forEach { movePackageDirs(it, oldFullPackage, newFullPackage) }

log("Refactoring completed successfully.")
log("New values: packagePrefix=$newPackagePrefix, packageName=$newPackageName, appName=$newAppName, fullPackage=$newFullPackage")
